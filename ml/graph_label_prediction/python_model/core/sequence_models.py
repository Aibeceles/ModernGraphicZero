"""
Model Architectures for RootList Sequence Prediction.

This module provides:
- NodeEncoder: GCN / GAT backbone producing per-node embeddings.
- PositionalEncoding: Standard sinusoidal positional encoding.
- SequenceEncoder: Transformer encoder over a window of node embeddings.
- RootListDecoder: Autoregressive Transformer decoder with separate
  size and root prediction heads.
- SequencePredictor: End-to-end model composing the above components.
"""

from typing import Optional, Tuple

import math
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv, GATConv

from .sequence_config import SequencePredictorConfig


# ---------------------------------------------------------------------------
# NodeEncoder
# ---------------------------------------------------------------------------

class NodeEncoder(nn.Module):
    """Graph encoder for node embeddings (2-layer GCN or GAT)."""

    def __init__(
        self,
        num_features: int,
        hidden_dim: int,
        encoder_type: str = 'gcn',
        dropout: float = 0.1,
        num_heads: int = 4,
    ):
        super().__init__()
        self.encoder_type = encoder_type
        self.dropout = dropout

        if encoder_type == 'gcn':
            self.conv1 = GCNConv(num_features, hidden_dim)
            self.conv2 = GCNConv(hidden_dim, hidden_dim)
            self.out_dim = hidden_dim
        elif encoder_type == 'gat':
            self.conv1 = GATConv(
                num_features, hidden_dim,
                heads=num_heads, concat=True, dropout=dropout,
            )
            self.conv2 = GATConv(
                hidden_dim * num_heads, hidden_dim,
                heads=1, concat=False, dropout=dropout,
            )
            self.out_dim = hidden_dim
        else:
            raise ValueError(f"Unknown encoder type: {encoder_type}")

    def forward(self, x: torch.Tensor, edge_index: torch.Tensor) -> torch.Tensor:
        """Return node embeddings ``[num_nodes, hidden_dim]``."""
        h = F.relu(self.conv1(x, edge_index))
        h = F.dropout(h, p=self.dropout, training=self.training)
        h = F.relu(self.conv2(h, edge_index))
        h = F.dropout(h, p=self.dropout, training=self.training)
        return h


# ---------------------------------------------------------------------------
# PositionalEncoding
# ---------------------------------------------------------------------------

class PositionalEncoding(nn.Module):
    """Sinusoidal positional encoding for a Transformer."""

    def __init__(self, d_model: int, max_len: int = 100, dropout: float = 0.1):
        super().__init__()
        self.dropout = nn.Dropout(p=dropout)

        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(
            torch.arange(0, d_model, 2).float() * (-math.log(10000.0) / d_model)
        )
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        pe = pe.unsqueeze(0)  # [1, max_len, d_model]
        self.register_buffer('pe', pe)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Add positional encoding: ``x`` has shape ``[batch, seq_len, d_model]``."""
        x = x + self.pe[:, :x.size(1), :]
        return self.dropout(x)


# ---------------------------------------------------------------------------
# SequenceEncoder
# ---------------------------------------------------------------------------

class SequenceEncoder(nn.Module):
    """Transformer encoder over a sequence of node embeddings."""

    def __init__(
        self,
        d_model: int,
        num_layers: int = 2,
        num_heads: int = 4,
        dim_feedforward: int = 256,
        dropout: float = 0.1,
    ):
        super().__init__()
        self.pos_encoding = PositionalEncoding(d_model, dropout=dropout)
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model,
            nhead=num_heads,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            batch_first=True,
        )
        self.encoder = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)

    def forward(
        self, x: torch.Tensor, mask: Optional[torch.Tensor] = None,
    ) -> torch.Tensor:
        """Encode ``[batch, seq_len, d_model]`` -> ``[batch, seq_len, d_model]``."""
        x = self.pos_encoding(x)
        return self.encoder(x, src_key_padding_mask=mask)


# ---------------------------------------------------------------------------
# RootListDecoder
# ---------------------------------------------------------------------------

class RootListDecoder(nn.Module):
    """Autoregressive Transformer decoder for RootList generation.

    Provides separate heads for predicting the list *size* and the
    individual *root* token IDs.
    """

    def __init__(
        self,
        vocab_size: int,
        d_model: int,
        max_size: int = 4,
        num_layers: int = 2,
        num_heads: int = 4,
        dim_feedforward: int = 256,
        dropout: float = 0.1,
    ):
        super().__init__()
        self.d_model = d_model
        self.max_size = max_size
        self.vocab_size = vocab_size

        self.token_embedding = nn.Embedding(vocab_size, d_model)
        self.size_embedding = nn.Embedding(max_size + 1, d_model)
        self.pos_encoding = PositionalEncoding(d_model, dropout=dropout)

        decoder_layer = nn.TransformerDecoderLayer(
            d_model=d_model,
            nhead=num_heads,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            batch_first=True,
        )
        self.decoder = nn.TransformerDecoder(decoder_layer, num_layers=num_layers)

        self.size_head = nn.Linear(d_model, max_size + 1)
        self.root_head = nn.Linear(d_model, vocab_size)

    @staticmethod
    def generate_causal_mask(seq_len: int, device: torch.device) -> torch.Tensor:
        mask = torch.triu(torch.ones(seq_len, seq_len, device=device), diagonal=1)
        return mask.masked_fill(mask == 1, float('-inf'))

    def forward(
        self,
        decoder_input: torch.Tensor,
        encoder_output: torch.Tensor,
        tgt_mask: Optional[torch.Tensor] = None,
        memory_mask: Optional[torch.Tensor] = None,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """Teacher-forcing forward pass.

        Args:
            decoder_input: ``[batch, seq_len]`` token IDs.
            encoder_output: ``[batch, context_len, d_model]``.

        Returns:
            ``(size_logits, root_logits)``
        """
        _, seq_len = decoder_input.shape
        x = self.token_embedding(decoder_input)
        x = self.pos_encoding(x)

        if tgt_mask is None:
            tgt_mask = self.generate_causal_mask(seq_len, decoder_input.device)

        decoded = self.decoder(
            x, encoder_output,
            tgt_mask=tgt_mask,
            memory_key_padding_mask=memory_mask,
        )

        size_logits = self.size_head(decoded)
        root_logits = self.root_head(decoded)
        return size_logits, root_logits


# ---------------------------------------------------------------------------
# SequencePredictor (full model)
# ---------------------------------------------------------------------------

class SequencePredictor(nn.Module):
    """End-to-end model: NodeEncoder + SequenceEncoder + RootListDecoder."""

    def __init__(self, config: SequencePredictorConfig):
        super().__init__()
        self.config = config

        self.node_encoder = NodeEncoder(
            num_features=config.num_node_features,
            hidden_dim=config.hidden_dim,
            encoder_type=config.encoder_type,
            dropout=config.dropout,
            num_heads=config.num_attention_heads,
        )

        self.seq_encoder = SequenceEncoder(
            d_model=config.hidden_dim,
            num_layers=config.num_encoder_layers,
            num_heads=config.num_attention_heads,
            dropout=config.dropout,
        )

        self.decoder = RootListDecoder(
            vocab_size=config.vocab_size,
            d_model=config.hidden_dim,
            max_size=config.max_rootlist_size,
            num_layers=config.num_decoder_layers,
            num_heads=config.num_attention_heads,
            dropout=config.dropout,
        )

        self.node_embeddings: Optional[torch.Tensor] = None

    def encode_graph(self, x: torch.Tensor, edge_index: torch.Tensor):
        """Compute and cache node embeddings for the full graph."""
        self.node_embeddings = self.node_encoder(x, edge_index)

    def forward(
        self,
        context_indices: torch.Tensor,
        decoder_input: torch.Tensor,
    ) -> Tuple[torch.Tensor, torch.Tensor]:
        """Forward pass for training.

        ``encode_graph()`` must be called first.

        Args:
            context_indices: ``[batch, context_len]`` global node indices.
            decoder_input: ``[batch, seq_len]`` decoder token IDs.

        Returns:
            ``(size_logits, root_logits)``
        """
        assert self.node_embeddings is not None, "Call encode_graph() first"

        context_embeds = self.node_embeddings[context_indices]
        encoder_output = self.seq_encoder(context_embeds)
        return self.decoder(decoder_input, encoder_output)
