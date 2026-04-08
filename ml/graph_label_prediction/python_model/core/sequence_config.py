"""
Configuration and Tokenizer for the RootList Sequence Prediction Pipeline.

This module provides:
- SequencePredictorConfig: Dataclass holding all hyperparameters for the
  sequence predictor (data, vocabulary, model architecture, training).
- RootListTokenizer: Encodes/decodes integer root values to/from token IDs
  and constructs decoder inputs for teacher-forced training.
"""

from dataclasses import dataclass
from typing import List

import torch


@dataclass
class SequencePredictorConfig:
    """Configuration for the RootList sequence predictor."""

    # Data settings
    context_window_size: int = 8

    # Vocabulary settings
    min_root_value: int = -82
    max_root_value: int = 49
    max_rootlist_size: int = 5

    # Model settings
    encoder_type: str = 'gcn'  # 'gcn' or 'gat'
    hidden_dim: int = 64
    num_encoder_layers: int = 2
    num_decoder_layers: int = 2
    num_attention_heads: int = 4
    dropout: float = 0.1

    # Node features (match existing pipeline)
    num_node_features: int = 16

    # Training settings
    learning_rate: float = 0.001
    weight_decay: float = 1e-4
    batch_size: int = 32
    max_epochs: int = 100
    patience: int = 10

    # Random seed
    random_seed: int = 42

    @property
    def vocab_size(self) -> int:
        """Total vocabulary size: integers + special tokens."""
        num_integers = self.max_root_value - self.min_root_value + 1
        num_special = 2  # SOS, PAD
        return num_integers + num_special

    @property
    def sos_token_id(self) -> int:
        """Start-of-sequence token ID."""
        return self.max_root_value - self.min_root_value + 1

    @property
    def pad_token_id(self) -> int:
        """Padding token ID."""
        return self.max_root_value - self.min_root_value + 2


class RootListTokenizer:
    """
    Tokenizer for RootList sequences.

    Maps integer root values to contiguous token IDs and back.
    Also provides helper methods for creating decoder inputs
    during teacher-forced training.
    """

    def __init__(self, config: SequencePredictorConfig):
        self.config = config
        self.min_val = config.min_root_value
        self.max_val = config.max_root_value

        self.sos_id = config.sos_token_id
        self.pad_id = config.pad_token_id
        self.vocab_size = config.vocab_size

    def encode_root(self, value: int) -> int:
        """Convert root integer to token ID."""
        return value - self.min_val

    def decode_root(self, token_id: int) -> int:
        """Convert token ID to root integer."""
        return token_id + self.min_val

    def encode_sequence(self, root_list: List[int], add_sos: bool = True) -> torch.Tensor:
        """Encode a RootList into token IDs.

        Args:
            root_list: List of integer roots.
            add_sos: Whether to prepend the SOS token.

        Returns:
            Token tensor of shape ``[len(root_list) + (1 if add_sos)]``.
        """
        tokens: List[int] = []
        if add_sos:
            tokens.append(self.sos_id)

        for root in root_list:
            tokens.append(self.encode_root(root))

        return torch.tensor(tokens, dtype=torch.long)

    def decode_sequence(self, tokens: torch.Tensor) -> List[int]:
        """Decode token IDs back to a RootList (excluding special tokens)."""
        result: List[int] = []
        for t in tokens.tolist():
            if t == self.sos_id or t == self.pad_id:
                continue
            if 0 <= t <= (self.max_val - self.min_val):
                result.append(self.decode_root(t))
        return result

    def create_decoder_input(self, target_tokens: torch.Tensor) -> torch.Tensor:
        """Create decoder input from target tokens (teacher forcing).

        Decoder input = ``[SOS] + target[:-1]``

        Args:
            target_tokens: Target sequence ``[batch, seq_len]``.

        Returns:
            Decoder input ``[batch, seq_len]``.
        """
        batch_size, seq_len = target_tokens.shape
        decoder_input = torch.full(
            (batch_size, seq_len), self.pad_id,
            dtype=torch.long, device=target_tokens.device,
        )
        decoder_input[:, 0] = self.sos_id
        decoder_input[:, 1:] = target_tokens[:, :-1]
        return decoder_input
