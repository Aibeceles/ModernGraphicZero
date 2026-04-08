"""Atomic file-claiming for concurrent pipeline.py workers.

Each pipeline invocation calls ``claim_next_parquet`` to exclusively lock one
Parquet file before processing.  The lock is a sidecar file ``<name>.lock``
created with ``O_CREAT | O_EXCL``, which is atomic on both Windows and POSIX —
only one process can create it; all others receive ``FileExistsError``.

Lifecycle
---------
1. ``claim_next_parquet``  — scan staging dir, atomically claim one file.
2. ``archive_and_release`` — on success: move parquet to ``done/``, delete lock.
3. ``release_claim``       — on failure: delete lock so another worker can retry.

Stale locks
-----------
Auto-release on failure covers normal exceptions.  A hard kill (SIGKILL /
Task Manager) can leave a stale ``.lock`` file.  To recover, delete the
orphaned ``<filename>.lock`` manually from the staging directory.
"""

import os
import shutil
from pathlib import Path
from typing import Optional


def _lock_path(parquet_path: str) -> str:
    """Return the sidecar lock-file path for a given parquet file path."""
    return parquet_path + ".lock"


def claim_next_parquet(batch_dir: str) -> Optional[str]:
    """Atomically claim the oldest unclaimed Parquet file in *batch_dir*.

    Scans *batch_dir* for ``*.parquet`` files (excluding the ``done/``
    subfolder).  Files are sorted by name, which sorts oldest-first because
    the Java writer embeds a ``yyyyMMdd_HHmmssSSS`` timestamp at the start of
    each filename.

    For each candidate the function attempts to create a ``.lock`` sidecar
    using ``O_CREAT | O_EXCL`` — an atomic operation on both Windows and POSIX.
    The first process to succeed owns that file; all others receive
    ``FileExistsError`` and move on to the next candidate.

    The PID of the claiming process is written to the lock file to aid
    diagnosis in case of a stale lock.

    Args:
        batch_dir: Path to the Parquet staging directory.

    Returns:
        Absolute path of the claimed ``.parquet`` file, or ``None`` if no
        unclaimed file is available.
    """
    staging = Path(batch_dir).resolve()
    done_dir = staging / "done"

    candidates = sorted(
        p for p in staging.glob("*.parquet")
        if p.parent != done_dir
    )

    for parquet_path in candidates:
        lock = Path(_lock_path(str(parquet_path)))
        try:
            fd = os.open(str(lock), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        except FileExistsError:
            continue

        try:
            os.write(fd, str(os.getpid()).encode())
        finally:
            os.close(fd)

        return str(parquet_path)

    return None


def release_claim(parquet_path: str) -> None:
    """Delete the lock sidecar for *parquet_path*, releasing the claim.

    Called on pipeline failure so another worker can retry the file.
    Safe to call even if the lock file no longer exists.

    Args:
        parquet_path: Path to the claimed ``.parquet`` file.
    """
    lock = Path(_lock_path(parquet_path))
    try:
        lock.unlink()
    except FileNotFoundError:
        pass


def archive_and_release(parquet_path: str, batch_dir: str) -> None:
    """Move *parquet_path* to the ``done/`` subfolder and delete its lock.

    Called on pipeline success.  Creates ``done/`` if it does not exist.
    If a file with the same name already exists in ``done/`` (e.g. from a
    previous partial run) it is overwritten.

    Args:
        parquet_path: Path to the claimed ``.parquet`` file.
        batch_dir: Path to the Parquet staging directory (parent of ``done/``).
    """
    done_dir = Path(batch_dir).resolve() / "done"
    done_dir.mkdir(exist_ok=True)

    src = Path(parquet_path)
    dst = done_dir / src.name
    shutil.move(str(src), str(dst))

    release_claim(parquet_path)
