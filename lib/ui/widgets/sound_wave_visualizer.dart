import 'dart:math';
import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';

class SoundWaveVisualizer extends StatefulWidget {
  final bool isPlaying;
  final int barCount;
  final double maxHeight;
  final Color? color;

  const SoundWaveVisualizer({
    super.key,
    required this.isPlaying,
    this.barCount = 4,
    this.maxHeight = 16.0,
    this.color,
  });

  @override
  State<SoundWaveVisualizer> createState() => _SoundWaveVisualizerState();
}

class _SoundWaveVisualizerState extends State<SoundWaveVisualizer>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    if (widget.isPlaying) {
      _controller.repeat(reverse: true);
    }
  }

  @override
  void didUpdateWidget(covariant SoundWaveVisualizer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isPlaying && !_controller.isAnimating) {
      _controller.repeat(reverse: true);
    } else if (!widget.isPlaying && _controller.isAnimating) {
      _controller.stop();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final barColor = widget.color ?? AppColors.primary;

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: List.generate(widget.barCount, (index) {
            double factor = 0.3;
            if (widget.isPlaying) {
              final phase = (index * 0.25);
              final wave = sin((_controller.value + phase) * pi);
              factor = 0.2 + 0.8 * wave.abs();
            }

            final height = (widget.maxHeight * factor).clamp(3.0, widget.maxHeight);

            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 1.5),
              width: 3.0,
              height: height,
              decoration: BoxDecoration(
                color: barColor,
                borderRadius: BorderRadius.circular(2),
                boxShadow: widget.isPlaying
                    ? [
                        BoxShadow(
                          color: barColor.withValues(alpha: 0.5),
                          blurRadius: 4,
                          spreadRadius: 0.5,
                        )
                      ]
                    : null,
              ),
            );
          }),
        );
      },
    );
  }
}
