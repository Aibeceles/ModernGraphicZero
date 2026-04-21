"""Build GraphicZero-TheQuadratics.ipynb (Python / pandas). Run: python build_graphic_zero_notebook.py"""
from __future__ import annotations

import json
from pathlib import Path


def cell_md(text: str) -> dict:
    return {"cell_type": "markdown", "metadata": {}, "source": [ln + "\n" for ln in text.strip("\n").split("\n")]}


def cell_code(text: str) -> dict:
    lines = text.strip("\n").split("\n")
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": [ln + "\n" for ln in lines],
    }


CELLS: list[tuple[str, str]] = [
    (
        "md",
        """# GraphicZero-TheQuadratics

Python port of the legacy Zeppelin notebook **`GraphicZero-TheQuadratics`** (id `2FZFR44WY`), runnable under the **Python 3** kernel (same as `quadratics.ipynb`).

**Data:** `extractS12` Parquet from `artifacts/parquet/strongestPath/<latest>/`.

**`z`:** same checkpoint names as Zeppelin.

**Legacy parity:** the load cell applies the **`s12QuadQ` fold** (degree `-1` → `0`, scalar ×2). Set **`ALIGN_LEGACY_ZEPPELIN_DIVISOR = True`** (default) to use divisor **2** like Neo4j `s12QuadQ` so numeric tables match the 2021 Zeppelin export (e.g. **128.0** for index 7). Set **`False`** to keep Parquet divisors (**1**) for the modern `ml/polys` extract.""",
    ),
    ("md", "The TwoPolynomialGenerator.jar output exhibited in detail."),
    (
        "md",
        """Description and method for defining quadratic representing 2^n:

N arguments a quadratic composed of an ordered set of quadratics. Each set member is derived from the sum-of-terms quadratic formula as consecutive pairs of denumerating integers argumenting linear terms and is complete when its size is N-1.

Each set member is then multiplied through by a scalar. From the first member, the scalar as 2^0 to the last member as 2^(n-1), the set is fully defined.

The resulting members are added together and resolve a single quadratic. When argumented with N this equation computes the expected result 2^n.""",
    ),
    (
        "code",
        r"""
from __future__ import annotations

import math
from decimal import Decimal, getcontext
from pathlib import Path

import pandas as pd
import pyarrow.parquet as pq

getcontext().prec = 50

z: dict[str, pd.DataFrame] = {}

candidate_parquet_roots = [
    Path("ml/polys/artifacts/parquet"),
    Path("../artifacts/parquet"),
    Path("artifacts/parquet"),
]
parquet_root = next((p for p in candidate_parquet_roots if p.exists()), None)
if parquet_root is None:
    raise FileNotFoundError("Could not find parquet root (see quadratics.ipynb paths).")

pipeline_name = "strongestPath"
latest = parquet_root / pipeline_name / "latest.txt"
if not latest.exists():
    raise FileNotFoundError(f"Missing {latest.resolve()}")
run_id = latest.read_text(encoding="utf-8").strip()
run_base = parquet_root / pipeline_name / run_id


def read_stage(name: str) -> pd.DataFrame:
    p = run_base / name
    if not p.exists():
        raise FileNotFoundError(p.resolve())
    return pq.read_table(str(p)).to_pandas()


print(f"pipeline={pipeline_name} run_id={run_id}")
print(f"run_base={run_base.resolve()}")
""",
    ),
    (
        "code",
        r"""
# === QuerryS12QuadQuerry.main — Parquet + optional legacy fold/divisor ===
s12 = read_stage("extractS12").copy()

_fold_deg, _fold_sc = [], []
for _, r in s12.iterrows():
    if str(r["degree"]) == "-1":
        _fold_deg.append("0")
        _fold_sc.append(str(int(r["scalar"]) * 2))
    else:
        _fold_deg.append(str(r["degree"]))
        _fold_sc.append(str(r["scalar"]))
s12["degree"] = _fold_deg
s12["scalar"] = _fold_sc

ALIGN_LEGACY_ZEPPELIN_DIVISOR = True
if ALIGN_LEGACY_ZEPPELIN_DIVISOR:
    s12["divisor"] = "2"

z["s12MaxN8"] = s12
print(f"s12MaxN8 rows={len(s12)} cols={list(s12.columns)}")
print(s12.head())
""",
    ),
    (
        "md",
        """### `PT_graphdb_scalar`-style pivot (TwoPolynomial042619.ods)

Spreadsheet view (matches **`PT_graphdb_scalar`** / **`TwoPolynomial042619.ods`**): pivot **`v.Degree`** **-1, 0, 1, 2** then **Total Result**.

- **Degree -1:** sum of **`v.Scalar * t.twoSeq`** (Laurent mass; no /2 in the -1 bucket, as in the ODS).
- **Degrees 0, 1, 2:** sum of **evaluated** terms **`(v.Scalar / 2) * i.MaxN^degree * t.twoSeq`** (same **`tDivisor = 2`** convention as `s12QuadQ`).

**Total Result** is the sum of those four columns. Uses **raw** `extractS12` (keeps **-1**).""",
    ),
    (
        "code",
        r"""
# === PT_graphdb_scalar-style pivot (TwoPolynomial042619.ods hybrid semantics) ===
raw = read_stage("extractS12").copy()
max_n = int(str(raw["maxN"].iloc[0]))
div2 = Decimal("2")


def ods_pivot_cell(r: pd.Series) -> Decimal:
    # ODS hybrid: deg -1 => scalar*rowScalar; else => (scalar/2)*maxN^deg*rowScalar.
    d = int(r["degree"])
    sc = Decimal(str(r["scalar"]))
    rs = Decimal(str(r["rowScalar"]))
    if d == -1:
        return sc * rs
    return (sc / div2) * (Decimal(max_n) ** d) * rs


raw["_cell"] = raw.apply(ods_pivot_cell, axis=1)
raw["deg"] = raw["degree"].astype(str)
agg = raw.groupby(["maxN", "index", "rowScalar", "deg"], as_index=False)["_cell"].sum()

pvt = agg.pivot(index=["maxN", "index", "rowScalar"], columns="deg", values="_cell")
pvt = pvt.fillna(Decimal(0))
for c in ("-1", "0", "1", "2"):
    if c not in pvt.columns:
        pvt[c] = Decimal(0)
pvt = pvt[["-1", "0", "1", "2"]]
pvt["Total Result"] = pvt["-1"] + pvt["0"] + pvt["1"] + pvt["2"]

pt_view = pvt.reset_index().rename(
    columns={
        "maxN": "i.MaxN",
        "index": "i.N",
        "rowScalar": "t.twoSeq",
        "-1": "v.Degree -1",
        "0": "v.Degree 0",
        "1": "v.Degree 1",
        "2": "v.Degree 2",
    }
)
z["PT_graphdb_scalar_pivot"] = pt_view

print("Sum — Result (ODS hybrid: -1 mass vs evaluated 0..2), pivot by v.Degree")
print(pt_view.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === ResultUDF.extendResult ===


def extend_result_row(r: pd.Series) -> pd.Series:
    max_n = int(r["maxN"])
    deg = int(r["degree"])
    sc = Decimal(str(r["scalar"]))
    div = Decimal(str(r["divisor"]))
    rs = Decimal(str(r["rowScalar"]))
    ev = (sc / div) * (Decimal(max_n) ** deg) * rs
    return pd.Series(
        {
            "dimension": "2",
            "degree": str(r["degree"]),
            "scalar": str(r["scalar"]),
            "index": str(r["index"]),
            "maxIndex": str(r["maxN"]),
            "divisor": str(r["divisor"]),
            "result": format(ev, "f"),
            "rowScalar": str(r["rowScalar"]),
        }
    )


src = z["s12MaxN8"]
rudf = src.apply(extend_result_row, axis=1)
z["rUdfMaxN8"] = rudf
print(f"rUdfMaxN8 rows={len(rudf)}")
print(rudf.head())
""",
    ),
    (
        "code",
        r"""
# === FilterIndex.main(Array("rUdfMaxN8", "7", "index7MaxN8")) ===
base = z["rUdfMaxN8"].copy()
base["index"] = base["index"].astype(str)
idx7 = base[base["index"].str.startswith("7")].reset_index(drop=True)
z["index7MaxN8"] = idx7
print(f"index7MaxN8 rows={len(idx7)}")
print(idx7.head())
""",
    ),
    (
        "code",
        r"""
# === IndexRowsSum ===
ds = z["index7MaxN8"].copy()
total = sum(Decimal(str(x)) for x in ds["result"])
out = pd.DataFrame({"index_Result": [str(total)]})
print(out.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === GroupedScalarRowsSum → KvDS ===
ds = z["index7MaxN8"].copy()
ds["rowScalar_i"] = ds["rowScalar"].astype(int)
ds["degree_i"] = ds["degree"].astype(int)
ds["scalar_i"] = ds["scalar"].astype(int)
kv = ds.groupby(["rowScalar_i", "degree_i"], as_index=False)["scalar_i"].sum()
kv = kv.rename(columns={"rowScalar_i": "rowScalar", "degree_i": "degree", "scalar_i": "scalar_Result"})
kv["scalar_Result"] = kv["scalar_Result"].astype(str)
z["KvDS"] = kv
print(kv.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === MapKVUDF → KvDSUnpavked ===
df = z["KvDS"].copy()
df["_1"] = df["rowScalar"].astype(int)
df["_2"] = df["degree"].astype(int)
z["KvDSUnpavked"] = df
print(df.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === RowScalarDegreePivot → pivotDF ===
df = z["KvDSUnpavked"].copy()
piv = df.pivot_table(index="rowScalar", columns="_2", values="scalar_Result", aggfunc="first")
piv = piv.rename(columns=lambda c: int(c) if not isinstance(c, str) else int(float(c)))
piv = piv.sort_index(axis=1)
for c in (0, 1, 2):
    if c not in piv.columns:
        piv[c] = pd.NA
piv = piv[[0, 1, 2]].reset_index().rename_axis(None, axis=1)
z["pivotDF"] = piv
print(piv.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === PivotEvaluateUDF → Rootsmaxn8n7 ===


def quad_equ(row_scalar: str, zero: str, one: str, two: str):
    four = Decimal(4)
    zero_d = Decimal(0)
    two_d = Decimal(2)
    row_scalar_d = Decimal(str(row_scalar))
    a = Decimal(str(two))
    a_mm = row_scalar_d * a
    a_m = a_mm / two_d
    two_a = a_m * two_d
    b = Decimal(str(one))
    b_mm = row_scalar_d * b
    b_m = b_mm / two_d
    neg_b = zero_d - b_m
    neg_bd_two_a = neg_b / two_a
    b_sqr = b_m**2
    c = Decimal(str(zero))
    c_mm = row_scalar_d * c
    c_m = c_mm / two_d
    ac = a_m * c_m
    fac = ac * four
    disc = b_sqr - fac
    disc_d = float(disc)
    if disc_d > 0:
        disc_sqr = Decimal(str(math.sqrt(disc_d)))
        disc_sqrd_two_a = disc_sqr / two_a
        root_one = neg_bd_two_a + disc_sqrd_two_a
        root_two = neg_bd_two_a - disc_sqrd_two_a
        return row_scalar, root_one, root_two, two, one, zero
    return row_scalar, "no", "root", two, one, zero


piv = z["pivotDF"].copy()
roots_rows = []
for _, r in piv.iterrows():
    rs = str(int(r["rowScalar"]))
    z0, z1, z2 = str(int(r[0])), str(int(r[1])), str(int(r[2]))
    row_scalar, root1, root2, a, b, c = quad_equ(rs, z0, z1, z2)
    roots_rows.append(
        {"rowScalar": row_scalar, "root1": str(root1), "root2": str(root2), "a": str(a), "b": str(b), "c": str(c)}
    )
roots_df = pd.DataFrame(roots_rows)
z["Rootsmaxn8n7"] = roots_df
print(roots_df.to_string(index=False))
""",
    ),
    (
        "md",
        """The graph quadratics appear as the definition asserts with two variations. First, the pairwise denumerating integers begin at MaxN though the set size and the associated scalar follow the definition. Second the rowScalar=1 quadratic is not defined in terms of a consecutive pair of denumerating integers. The dataframe below suggests the rowScalar=1 quadratic is actually two quadratics continuing the denumerating pairs definition along with a constant equal to 2N..""",
    ),
    (
        "code",
        r"""
# === GroupedDegreeSum → KvDegreeDS ===
ds = z["index7MaxN8"].copy()
ds["_w"] = ds.apply(
    lambda r: Decimal(str(r["scalar"])) * Decimal(str(r["rowScalar"])) / Decimal(str(r["divisor"])),
    axis=1,
)
ds["maxIndex_i"] = ds["maxIndex"].astype(int)
ds["degree_i"] = ds["degree"].astype(int)
g = ds.groupby(["maxIndex_i", "degree_i"], as_index=False)["_w"].sum()
g = g.rename(columns={"maxIndex_i": "key_1", "degree_i": "key_2", "_w": "scalar_Result"})
g["scalar_Result"] = g["scalar_Result"].map(lambda x: format(x, "f"))
z["KvDegreeDS"] = g
print(g.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === MapKVUDF (degree path) ===
df = z["KvDegreeDS"].copy()
df["_1"] = df["key_1"].astype(int)
df["_2"] = df["key_2"].astype(int)
z["KvDegreeDSUnpavked"] = df
print(df.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === DropKeyColumn → ReducedMaxN8N7 ===
df = z["KvDegreeDSUnpavked"].copy()
out = pd.DataFrame(
    {
        "Scalar": df["scalar_Result"].astype(str),
        "MaxN": df["_1"].astype(str),
        "Degree": df["_2"].astype(str),
    }
)
z["ReducedMaxN8N7"] = out
print(out.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === AggrigatedScalarDegreePivot → ReducedMaxN8N7Pivot ===
dh = z["ReducedMaxN8N7"].copy()
dh["Scalar_d"] = dh["Scalar"].apply(lambda s: Decimal(str(s)))
agg = dh.pivot_table(index="MaxN", columns="Degree", values="Scalar_d", aggfunc="sum")
agg = agg.rename(columns=str)
for c in ("0", "1", "2"):
    if c not in agg.columns:
        agg[c] = Decimal(0)
agg = agg[["0", "1", "2"]].reset_index()
z["ReducedMaxN8N7Pivot"] = agg
print(agg.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === ReducedResultUDF + pivot + ReducedRowsSum ===
db = z["ReducedMaxN8N7"].copy()
rows = []
for _, r in db.iterrows():
    sc = Decimal(str(r["Scalar"]))
    m = int(float(r["MaxN"]))
    d = int(float(r["Degree"]))
    res = sc * (Decimal(m) ** d)
    rows.append(
        {"Scalar": str(r["Scalar"]), "MaxN": str(r["MaxN"]), "Degree": str(r["Degree"]), "result": format(res, "f")}
    )
red = pd.DataFrame(rows)
z["ReducedMaxN8N7Result"] = red
print(red.to_string(index=False))

red["result_d"] = red["result"].apply(lambda s: Decimal(str(s)))
piv = red.pivot_table(index="MaxN", columns="Degree", values="result_d", aggfunc="sum")
for c in ("0", "1", "2"):
    if str(c) not in piv.columns:
        piv[str(c)] = Decimal(0)
piv = piv[["0", "1", "2"]].reset_index()
z["ReducedMaxN8N7ResultPivot"] = piv
print(piv.to_string(index=False))

final_sum = sum(Decimal(str(x)) for x in red["result"])
z["ReducedMaxN8N7FinalResult"] = pd.DataFrame({"index_Result": [format(final_sum, "f")]})
print(z["ReducedMaxN8N7FinalResult"].to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === FilterRowScalar → index7MaxN8RowScalar1 ===
df = z["index7MaxN8"].copy()
f1 = df[df["rowScalar"].astype(str) == "1"].reset_index(drop=True)
z["index7MaxN8RowScalar1"] = f1
print(f1.to_string(index=False))
""",
    ),
    (
        "code",
        r"""
# === Zeppelin showcase z1–z4 ===
print("Rootsmaxn8n7")
print(z["Rootsmaxn8n7"].to_string(index=False))
print("\nReducedMaxN8N7Pivot")
print(z["ReducedMaxN8N7Pivot"].to_string(index=False))
print("\nReducedMaxN8N7ResultPivot")
print(z["ReducedMaxN8N7ResultPivot"].to_string(index=False))
print("\nReducedMaxN8N7FinalResult")
print(z["ReducedMaxN8N7FinalResult"].to_string(index=False))
""",
    ),
]


def main() -> None:
    nb = {
        "cells": [],
        "metadata": {
            "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
            "language_info": {"name": "python", "version": "3.12.0"},
        },
        "nbformat": 4,
        "nbformat_minor": 5,
    }
    C = nb["cells"]
    for kind, text in CELLS:
        C.append(cell_md(text) if kind == "md" else cell_code(text))

    out = Path(__file__).resolve().parent / "GraphicZero-TheQuadratics.ipynb"
    out.write_text(json.dumps(nb, indent=1), encoding="utf-8")
    print("wrote", out)


if __name__ == "__main__":
    main()
