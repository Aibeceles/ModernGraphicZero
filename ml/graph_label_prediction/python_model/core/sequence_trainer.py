"""
Training, Inference, and Evaluation for RootList Sequence Prediction.

This module provides:
- compute_loss: Combined size + root cross-entropy loss.
- train_epoch / evaluate: Single-epoch training and evaluation loops.
- train_model: Full training loop with early stopping.
- predict_rootlist / batch_predict: Autoregressive inference.
- compute_metrics / print_evaluation_report: Evaluation metrics and reporting.
"""

from collections import defaultdict
from typing import Dict, List, Optional, Tuple

import torch
import torch.nn.functional as F
from torch.optim import Adam
from torch.utils.data import DataLoader
from torch_geometric.data import Data

from .sequence_config import SequencePredictorConfig, RootListTokenizer
from .sequence_models import SequencePredictor


# ---------------------------------------------------------------------------
# Loss
# ---------------------------------------------------------------------------

def compute_loss(
    size_logits: torch.Tensor,
    root_logits: torch.Tensor,
    target_tokens: torch.Tensor,
    target_size: torch.Tensor,
    target_mask: torch.Tensor,
    config: SequencePredictorConfig,
) -> Tuple[torch.Tensor, Dict[str, float]]:
    """Compute combined size + root cross-entropy loss."""

    size_pred = size_logits[:, 0, :]
    size_loss = F.cross_entropy(size_pred, target_size)

    root_pred = root_logits[:, 1:, :]
    root_targets = target_tokens[:, 1:]
    root_mask = target_mask[:, 1:]

    root_pred_flat = root_pred.reshape(-1, config.vocab_size)
    root_targets_flat = root_targets.reshape(-1)
    root_mask_flat = root_mask.reshape(-1)

    if root_mask_flat.any():
        root_loss = F.cross_entropy(
            root_pred_flat[root_mask_flat],
            root_targets_flat[root_mask_flat],
            ignore_index=config.pad_token_id,
        )
    else:
        root_loss = torch.tensor(0.0, device=size_logits.device)

    total_loss = size_loss + root_loss
    return total_loss, {
        'total': total_loss.item(),
        'size': size_loss.item(),
        'root': root_loss.item(),
    }


# ---------------------------------------------------------------------------
# Training helpers
# ---------------------------------------------------------------------------

def train_epoch(
    model: SequencePredictor,
    train_loader: DataLoader,
    optimizer: torch.optim.Optimizer,
    pyg_data: Data,
    tokenizer: RootListTokenizer,
    config: SequencePredictorConfig,
    device: str,
) -> Dict[str, float]:
    """Train for one epoch. Returns average losses."""

    model.train()
    total_losses: Dict[str, float] = {'total': 0.0, 'size': 0.0, 'root': 0.0}
    num_batches = 0

    pyg_data = pyg_data.to(device)

    for batch in train_loader:
        context_indices = batch['context_indices'].to(device)
        target_tokens = batch['target_tokens'].to(device)
        target_size = batch['target_size'].to(device)
        target_mask = batch['target_mask'].to(device)

        decoder_input = tokenizer.create_decoder_input(target_tokens)

        optimizer.zero_grad()
        model.encode_graph(pyg_data.x, pyg_data.edge_index)
        size_logits, root_logits = model(context_indices, decoder_input)

        loss, loss_dict = compute_loss(
            size_logits, root_logits,
            target_tokens, target_size, target_mask,
            config,
        )

        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        optimizer.step()

        for k, v in loss_dict.items():
            total_losses[k] += v
        num_batches += 1

    return {k: v / num_batches for k, v in total_losses.items()}


@torch.no_grad()
def evaluate(
    model: SequencePredictor,
    data_loader: DataLoader,
    pyg_data: Data,
    tokenizer: RootListTokenizer,
    config: SequencePredictorConfig,
    device: str,
) -> Dict[str, float]:
    """Evaluate model on a dataset. Returns average losses."""

    model.eval()
    total_losses: Dict[str, float] = {'total': 0.0, 'size': 0.0, 'root': 0.0}
    num_batches = 0

    pyg_data = pyg_data.to(device)
    model.encode_graph(pyg_data.x, pyg_data.edge_index)

    for batch in data_loader:
        context_indices = batch['context_indices'].to(device)
        target_tokens = batch['target_tokens'].to(device)
        target_size = batch['target_size'].to(device)
        target_mask = batch['target_mask'].to(device)

        decoder_input = tokenizer.create_decoder_input(target_tokens)
        size_logits, root_logits = model(context_indices, decoder_input)

        _, loss_dict = compute_loss(
            size_logits, root_logits,
            target_tokens, target_size, target_mask,
            config,
        )

        for k, v in loss_dict.items():
            total_losses[k] += v
        num_batches += 1

    return {k: v / num_batches for k, v in total_losses.items()}


# ---------------------------------------------------------------------------
# Full training loop
# ---------------------------------------------------------------------------

def train_model(
    model: SequencePredictor,
    train_loader: DataLoader,
    val_loader: DataLoader,
    pyg_data: Data,
    tokenizer: RootListTokenizer,
    config: SequencePredictorConfig,
    device: str,
) -> Tuple[SequencePredictor, Dict]:
    """Full training loop with early stopping.

    Returns:
        ``(best_model, history)``
    """

    optimizer = Adam(
        model.parameters(),
        lr=config.learning_rate,
        weight_decay=config.weight_decay,
    )

    best_val_loss = float('inf')
    patience_counter = 0
    best_model_state = None

    history: Dict[str, list] = {
        'train_loss': [],
        'val_loss': [],
        'train_size_loss': [],
        'val_size_loss': [],
        'train_root_loss': [],
        'val_root_loss': [],
    }

    print(f"Starting training for up to {config.max_epochs} epochs...")
    print(f"  Early stopping patience: {config.patience}")
    print()

    for epoch in range(config.max_epochs):
        train_losses = train_epoch(
            model, train_loader, optimizer,
            pyg_data, tokenizer, config, device,
        )
        val_losses = evaluate(
            model, val_loader,
            pyg_data, tokenizer, config, device,
        )

        history['train_loss'].append(train_losses['total'])
        history['val_loss'].append(val_losses['total'])
        history['train_size_loss'].append(train_losses['size'])
        history['val_size_loss'].append(val_losses['size'])
        history['train_root_loss'].append(train_losses['root'])
        history['val_root_loss'].append(val_losses['root'])

        if epoch % 5 == 0 or epoch == config.max_epochs - 1:
            print(
                f"Epoch {epoch:3d} | "
                f"Train Loss: {train_losses['total']:.4f} "
                f"(size: {train_losses['size']:.4f}, root: {train_losses['root']:.4f}) | "
                f"Val Loss: {val_losses['total']:.4f}"
            )

        if val_losses['total'] < best_val_loss:
            best_val_loss = val_losses['total']
            patience_counter = 0
            best_model_state = {
                k: v.cpu().clone() for k, v in model.state_dict().items()
            }
        else:
            patience_counter += 1
            if patience_counter >= config.patience:
                print(f"\nEarly stopping at epoch {epoch}")
                break

    if best_model_state is not None:
        model.load_state_dict(best_model_state)

    print(f"\nTraining complete. Best validation loss: {best_val_loss:.4f}")
    return model, history


# ---------------------------------------------------------------------------
# Inference
# ---------------------------------------------------------------------------

@torch.no_grad()
def predict_rootlist(
    model: SequencePredictor,
    context_indices: torch.Tensor,
    tokenizer: RootListTokenizer,
    config: SequencePredictorConfig,
    use_predicted_size: bool = True,
    known_size: Optional[int] = None,
) -> Tuple[int, List[int]]:
    """Autoregressively generate a RootList prediction for a single sample."""

    model.eval()
    device = next(model.parameters()).device

    if context_indices.dim() == 1:
        context_indices = context_indices.unsqueeze(0)
    context_indices = context_indices.to(device)

    generated = [tokenizer.sos_id]

    context_embeds = model.node_embeddings[context_indices]
    encoder_output = model.seq_encoder(context_embeds)

    decoder_input = torch.tensor([[tokenizer.sos_id]], device=device)
    size_logits, _ = model.decoder(decoder_input, encoder_output)

    if use_predicted_size:
        predicted_size = size_logits[0, 0].argmax().item()
    else:
        predicted_size = known_size if known_size is not None else 1

    predicted_roots: List[int] = []
    for _ in range(predicted_size):
        decoder_input = torch.tensor([generated], device=device)
        _, root_logits = model.decoder(decoder_input, encoder_output)

        next_token = root_logits[0, -1].argmax().item()
        if next_token >= tokenizer.sos_id:
            valid_logits = root_logits[0, -1, :tokenizer.sos_id]
            next_token = valid_logits.argmax().item()

        generated.append(next_token)
        predicted_roots.append(tokenizer.decode_root(next_token))

    return predicted_size, predicted_roots


@torch.no_grad()
def batch_predict(
    model: SequencePredictor,
    data_loader: DataLoader,
    pyg_data: Data,
    tokenizer: RootListTokenizer,
    config: SequencePredictorConfig,
    device: str,
) -> List[Dict]:
    """Generate predictions for a whole dataset."""

    model.eval()
    predictions: List[Dict] = []

    pyg_data = pyg_data.to(device)
    model.encode_graph(pyg_data.x, pyg_data.edge_index)

    for batch in data_loader:
        context_indices = batch['context_indices'].to(device)
        target_tokens = batch['target_tokens']
        target_size = batch['target_size']
        batch_size = context_indices.shape[0]

        for i in range(batch_size):
            pred_size, pred_roots = predict_rootlist(
                model, context_indices[i:i + 1], tokenizer, config,
            )
            true_size = target_size[i].item()
            true_tokens = target_tokens[i, 1:true_size + 1].tolist()
            true_roots = [
                tokenizer.decode_root(t)
                for t in true_tokens if t < tokenizer.sos_id
            ]
            predictions.append({
                'predicted_size': pred_size,
                'predicted_roots': pred_roots,
                'true_size': true_size,
                'true_roots': true_roots,
            })

    return predictions


# ---------------------------------------------------------------------------
# Metrics
# ---------------------------------------------------------------------------

def compute_metrics(predictions: List[Dict]) -> Dict[str, float]:
    """Compute size_accuracy, exact_match, set_match, element_accuracy."""

    size_correct = exact_match = set_match = 0
    element_correct = element_total = 0

    for pred in predictions:
        pred_roots = pred['predicted_roots']
        true_roots = pred['true_roots']

        if pred['predicted_size'] == pred['true_size']:
            size_correct += 1
        if pred_roots == true_roots:
            exact_match += 1
        if set(pred_roots) == set(true_roots):
            set_match += 1

        min_len = min(len(pred_roots), len(true_roots))
        for j in range(min_len):
            if pred_roots[j] == true_roots[j]:
                element_correct += 1
        element_total += max(len(true_roots), 1)

    n = len(predictions)
    return {
        'size_accuracy': size_correct / n if n else 0.0,
        'exact_match': exact_match / n if n else 0.0,
        'set_match': set_match / n if n else 0.0,
        'element_accuracy': element_correct / element_total if element_total else 0.0,
        'num_samples': n,
    }


def print_evaluation_report(
    predictions: List[Dict], split_name: str = 'Test',
) -> Dict[str, float]:
    """Print a formatted evaluation report and return metrics."""

    metrics = compute_metrics(predictions)

    print(f"\n{'=' * 60}")
    print(f"{split_name} Set Evaluation Report")
    print(f"{'=' * 60}")
    print(f"Number of samples: {metrics['num_samples']}")
    print()
    print(f"Size Accuracy:     {metrics['size_accuracy'] * 100:.2f}%")
    print(f"Exact Match:       {metrics['exact_match'] * 100:.2f}%")
    print(f"Set Match:         {metrics['set_match'] * 100:.2f}%")
    print(f"Element Accuracy:  {metrics['element_accuracy'] * 100:.2f}%")
    print(f"{'=' * 60}")

    print(f"\nExample predictions:")
    for pred in predictions[:5]:
        sym = "\u2713" if pred['predicted_roots'] == pred['true_roots'] else "\u2717"
        print(f"  {sym} Pred: size={pred['predicted_size']}, roots={pred['predicted_roots']}")
        print(f"    True: size={pred['true_size']}, roots={pred['true_roots']}")

    return metrics


def print_per_size_analysis(predictions: List[Dict]):
    """Print exact-match accuracy broken down by true RootList size."""

    size_analysis: Dict[int, Dict[str, int]] = defaultdict(lambda: {'correct': 0, 'total': 0})
    for pred in predictions:
        true_size = pred['true_size']
        size_analysis[true_size]['total'] += 1
        if pred['predicted_roots'] == pred['true_roots']:
            size_analysis[true_size]['correct'] += 1

    print("\nExact Match Accuracy by True RootList Size:")
    print("-" * 40)
    for size in sorted(size_analysis):
        data = size_analysis[size]
        acc = data['correct'] / data['total'] if data['total'] else 0
        print(f"  Size {size}: {acc * 100:.1f}% ({data['correct']}/{data['total']})")
