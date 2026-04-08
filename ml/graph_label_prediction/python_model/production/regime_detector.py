"""
Regime Detection and Drift Monitoring via FAISS Vector Indexing.

This module provides embedding-based regime detection for:
1. Regime Detection: Identify which class region a new embedding belongs to
2. Drift Monitoring: Flag embeddings far from the indexed manifold
3. Explanation: Find k-nearest neighbors for interpretability

Vector indexing is NOT for prediction - it's for confidence adjustment
and operational monitoring.

Usage:
    # Fit on training data
    detector = RegimeDetector(embedding_dim=256)
    detector.fit(train_embeddings, train_labels)
    
    # Detect regime for new embeddings
    regime, distance = detector.detect_regime(new_embeddings)
    
    # Check for drift
    drift_mask = detector.detect_drift(new_embeddings, threshold=1.5)
    
    # Explain predictions
    neighbors, distances = detector.explain(embedding, k=5)
"""

from dataclasses import dataclass
from typing import Optional, Tuple, List, Dict
import numpy as np

try:
    import faiss
    FAISS_AVAILABLE = True
except ImportError:
    FAISS_AVAILABLE = False
    faiss = None


@dataclass
class RegimeInfo:
    """Information about a regime (class) in embedding space."""
    label: int
    centroid: np.ndarray
    radius: float  # Mean distance from centroid
    count: int  # Number of samples
    std: float  # Standard deviation of distances


@dataclass
class RegimeDetectionOutput:
    """Output from regime detection."""
    nearest_regime: np.ndarray  # Predicted regime [N]
    distance_to_regime: np.ndarray  # Distance to nearest centroid [N]
    is_out_of_distribution: np.ndarray  # Boolean mask [N]
    regime_confidences: np.ndarray  # Softmax over distances [N, num_regimes]


class RegimeDetector:
    """
    FAISS-based regime detection and drift monitoring.
    
    Indexes embeddings from training data and provides:
    - Regime detection via nearest centroid
    - Drift detection via k-NN distance
    - Explanation via nearest neighbor retrieval
    
    Uses normalized embeddings with inner product (cosine similarity).
    
    Args:
        embedding_dim: Dimension of embeddings
        num_regimes: Number of class regimes (default: 5)
        use_gpu: Whether to use GPU for FAISS (if available)
    """
    
    def __init__(
        self,
        embedding_dim: int,
        num_regimes: int = 5,
        use_gpu: bool = False,
    ):
        if not FAISS_AVAILABLE:
            raise ImportError(
                "FAISS is required for RegimeDetector. "
                "Install with: pip install faiss-cpu (or faiss-gpu)"
            )
        
        self.embedding_dim = embedding_dim
        self.num_regimes = num_regimes
        self.use_gpu = use_gpu
        
        # Main index for all embeddings
        self.index: Optional[faiss.Index] = None
        
        # Per-regime indices for targeted search
        self.regime_indices: Dict[int, faiss.Index] = {}
        
        # Regime information
        self.regime_info: Dict[int, RegimeInfo] = {}
        
        # Labels for indexed embeddings (for explanation)
        self.indexed_labels: Optional[np.ndarray] = None
        
        # Node IDs for indexed embeddings (for explanation)
        self.indexed_ids: Optional[np.ndarray] = None
        
        # Whether the detector has been fit
        self._fitted = False
    
    def fit(
        self,
        embeddings: np.ndarray,
        labels: np.ndarray,
        node_ids: Optional[np.ndarray] = None,
    ):
        """
        Index training embeddings and compute regime statistics.
        
        Args:
            embeddings: Training embeddings [N, embedding_dim]
            labels: Class labels [N]
            node_ids: Optional node IDs for explanation [N]
        """
        N = embeddings.shape[0]
        
        # Normalize embeddings for cosine similarity
        embeddings_norm = self._normalize(embeddings)
        
        # Create main index (inner product on normalized = cosine similarity)
        self.index = faiss.IndexFlatIP(self.embedding_dim)
        
        if self.use_gpu and faiss.get_num_gpus() > 0:
            res = faiss.StandardGpuResources()
            self.index = faiss.index_cpu_to_gpu(res, 0, self.index)
        
        self.index.add(embeddings_norm.astype(np.float32))
        
        # Store labels and IDs
        self.indexed_labels = labels.copy()
        self.indexed_ids = node_ids.copy() if node_ids is not None else np.arange(N)
        
        # Compute per-regime statistics and indices
        for regime in range(self.num_regimes):
            mask = labels == regime
            if not mask.any():
                continue
            
            regime_embeddings = embeddings_norm[mask]
            
            # Centroid (normalized)
            centroid = regime_embeddings.mean(axis=0)
            centroid = centroid / (np.linalg.norm(centroid) + 1e-8)
            
            # Distances from centroid (1 - cosine similarity)
            similarities = regime_embeddings @ centroid
            distances = 1 - similarities
            
            self.regime_info[regime] = RegimeInfo(
                label=regime,
                centroid=centroid,
                radius=float(distances.mean()),
                count=int(mask.sum()),
                std=float(distances.std()),
            )
            
            # Per-regime index
            regime_index = faiss.IndexFlatIP(self.embedding_dim)
            regime_index.add(regime_embeddings.astype(np.float32))
            self.regime_indices[regime] = regime_index
        
        self._fitted = True
    
    def detect_regime(
        self,
        embeddings: np.ndarray,
        return_all: bool = False,
    ) -> Tuple[np.ndarray, np.ndarray]:
        """
        Detect which regime each embedding belongs to.
        
        Args:
            embeddings: Query embeddings [N, embedding_dim]
            return_all: If True, return full RegimeDetectionOutput
            
        Returns:
            nearest_regime: Predicted regime [N]
            distance_to_regime: Distance to nearest centroid [N]
        """
        self._check_fitted()
        
        embeddings_norm = self._normalize(embeddings)
        N = embeddings_norm.shape[0]
        
        # Compute distances to all centroids
        centroids = np.stack([
            self.regime_info[r].centroid 
            for r in range(self.num_regimes) 
            if r in self.regime_info
        ])
        
        # Cosine similarity to centroids
        similarities = embeddings_norm @ centroids.T  # [N, num_regimes]
        distances = 1 - similarities  # Convert to distance
        
        # Nearest regime
        nearest_regime = distances.argmin(axis=1)
        min_distances = distances.min(axis=1)
        
        if return_all:
            # Check for OOD (distance > mean + 2*std for nearest regime)
            ood_mask = np.zeros(N, dtype=bool)
            for i in range(N):
                regime = nearest_regime[i]
                if regime in self.regime_info:
                    info = self.regime_info[regime]
                    threshold = info.radius + 2 * info.std
                    ood_mask[i] = min_distances[i] > threshold
            
            # Softmax over negative distances for regime confidence
            regime_confidences = np.exp(-distances * 5)  # Temperature scaling
            regime_confidences = regime_confidences / regime_confidences.sum(axis=1, keepdims=True)
            
            return RegimeDetectionOutput(
                nearest_regime=nearest_regime,
                distance_to_regime=min_distances,
                is_out_of_distribution=ood_mask,
                regime_confidences=regime_confidences,
            )
        
        return nearest_regime, min_distances
    
    def detect_drift(
        self,
        embeddings: np.ndarray,
        k: int = 5,
        threshold: Optional[float] = None,
    ) -> Tuple[np.ndarray, np.ndarray]:
        """
        Detect drift by checking k-NN distances.
        
        Embeddings far from the indexed manifold are flagged as drift.
        
        Args:
            embeddings: Query embeddings [N, embedding_dim]
            k: Number of nearest neighbors
            threshold: Distance threshold (default: adaptive based on training)
            
        Returns:
            drift_mask: Boolean mask indicating drift [N]
            mean_distances: Mean k-NN distance [N]
        """
        self._check_fitted()
        
        embeddings_norm = self._normalize(embeddings)
        
        # Search k nearest neighbors
        similarities, indices = self.index.search(
            embeddings_norm.astype(np.float32), k
        )
        
        # Convert similarities to distances (1 - cosine_sim)
        distances = 1 - similarities
        mean_distances = distances.mean(axis=1)
        
        # Adaptive threshold if not provided
        if threshold is None:
            # Use mean + 2*std of training distances
            # Approximate from regime radii
            all_radii = [info.radius for info in self.regime_info.values()]
            all_stds = [info.std for info in self.regime_info.values()]
            threshold = np.mean(all_radii) + 2 * np.mean(all_stds)
        
        drift_mask = mean_distances > threshold
        
        return drift_mask, mean_distances
    
    def explain(
        self,
        embeddings: np.ndarray,
        k: int = 5,
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        """
        Find k nearest neighbors for explanation.
        
        Args:
            embeddings: Query embeddings [N, embedding_dim]
            k: Number of nearest neighbors
            
        Returns:
            neighbor_indices: Indices in training set [N, k]
            neighbor_distances: Distances to neighbors [N, k]
            neighbor_labels: Labels of neighbors [N, k]
            neighbor_ids: Node IDs of neighbors [N, k]
        """
        self._check_fitted()
        
        embeddings_norm = self._normalize(embeddings)
        
        # Search k nearest neighbors
        similarities, indices = self.index.search(
            embeddings_norm.astype(np.float32), k
        )
        
        # Convert similarities to distances
        distances = 1 - similarities
        
        # Get labels and IDs of neighbors
        neighbor_labels = self.indexed_labels[indices]
        neighbor_ids = self.indexed_ids[indices]
        
        return indices, distances, neighbor_labels, neighbor_ids
    
    def get_regime_stats(self) -> Dict[int, Dict]:
        """
        Get statistics for each regime.
        
        Returns:
            Dictionary mapping regime to statistics
        """
        self._check_fitted()
        
        return {
            regime: {
                'count': info.count,
                'radius': info.radius,
                'std': info.std,
            }
            for regime, info in self.regime_info.items()
        }
    
    def save(self, path: str):
        """
        Save the detector to disk.
        
        Args:
            path: Path prefix for saved files
        """
        self._check_fitted()
        
        # Save FAISS index
        if self.use_gpu:
            index_cpu = faiss.index_gpu_to_cpu(self.index)
            faiss.write_index(index_cpu, f"{path}_index.faiss")
        else:
            faiss.write_index(self.index, f"{path}_index.faiss")
        
        # Save metadata
        np.savez(
            f"{path}_metadata.npz",
            indexed_labels=self.indexed_labels,
            indexed_ids=self.indexed_ids,
            embedding_dim=self.embedding_dim,
            num_regimes=self.num_regimes,
        )
        
        # Save regime info
        regime_data = {
            f"centroid_{r}": info.centroid
            for r, info in self.regime_info.items()
        }
        regime_data.update({
            f"radius_{r}": info.radius
            for r, info in self.regime_info.items()
        })
        regime_data.update({
            f"std_{r}": info.std
            for r, info in self.regime_info.items()
        })
        regime_data.update({
            f"count_{r}": info.count
            for r, info in self.regime_info.items()
        })
        np.savez(f"{path}_regimes.npz", **regime_data)
    
    @classmethod
    def load(cls, path: str, use_gpu: bool = False) -> "RegimeDetector":
        """
        Load a detector from disk.
        
        Args:
            path: Path prefix for saved files
            use_gpu: Whether to use GPU
            
        Returns:
            Loaded RegimeDetector
        """
        # Load metadata
        metadata = np.load(f"{path}_metadata.npz")
        embedding_dim = int(metadata['embedding_dim'])
        num_regimes = int(metadata['num_regimes'])
        
        # Create detector
        detector = cls(
            embedding_dim=embedding_dim,
            num_regimes=num_regimes,
            use_gpu=use_gpu,
        )
        
        # Load index
        detector.index = faiss.read_index(f"{path}_index.faiss")
        if use_gpu and faiss.get_num_gpus() > 0:
            res = faiss.StandardGpuResources()
            detector.index = faiss.index_cpu_to_gpu(res, 0, detector.index)
        
        # Load labels and IDs
        detector.indexed_labels = metadata['indexed_labels']
        detector.indexed_ids = metadata['indexed_ids']
        
        # Load regime info
        regime_data = np.load(f"{path}_regimes.npz")
        for r in range(num_regimes):
            if f"centroid_{r}" in regime_data:
                detector.regime_info[r] = RegimeInfo(
                    label=r,
                    centroid=regime_data[f"centroid_{r}"],
                    radius=float(regime_data[f"radius_{r}"]),
                    count=int(regime_data[f"count_{r}"]),
                    std=float(regime_data[f"std_{r}"]),
                )
        
        detector._fitted = True
        return detector
    
    def _normalize(self, embeddings: np.ndarray) -> np.ndarray:
        """Normalize embeddings to unit length."""
        norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
        return embeddings / (norms + 1e-8)
    
    def _check_fitted(self):
        """Check if detector has been fit."""
        if not self._fitted:
            raise RuntimeError(
                "RegimeDetector has not been fit. Call fit() first."
            )


def compute_regime_boundaries(
    embeddings: np.ndarray,
    labels: np.ndarray,
    method: str = 'centroid',
) -> Dict[Tuple[int, int], float]:
    """
    Compute boundary distances between regimes.
    
    Useful for understanding regime overlap and setting thresholds.
    
    Args:
        embeddings: Training embeddings [N, embedding_dim]
        labels: Class labels [N]
        method: 'centroid' or 'nearest' boundary computation
        
    Returns:
        Dictionary mapping (regime_i, regime_j) to boundary distance
    """
    # Normalize embeddings
    norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
    embeddings_norm = embeddings / (norms + 1e-8)
    
    unique_labels = np.unique(labels)
    
    # Compute centroids
    centroids = {}
    for label in unique_labels:
        mask = labels == label
        centroid = embeddings_norm[mask].mean(axis=0)
        centroid = centroid / (np.linalg.norm(centroid) + 1e-8)
        centroids[label] = centroid
    
    # Compute pairwise distances
    boundaries = {}
    for i in unique_labels:
        for j in unique_labels:
            if i >= j:
                continue
            
            if method == 'centroid':
                # Distance between centroids
                dist = 1 - np.dot(centroids[i], centroids[j])
            else:
                # Minimum distance between points
                mask_i = labels == i
                mask_j = labels == j
                sims = embeddings_norm[mask_i] @ embeddings_norm[mask_j].T
                dist = 1 - sims.max()
            
            boundaries[(i, j)] = float(dist)
    
    return boundaries
