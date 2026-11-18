import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

/// Clean SVG Vector Icon widget replacing standard icon fonts across the player.
class AppSvgIcon extends StatelessWidget {
  final String svg;
  final double size;
  final Color? color;
  final String? semanticsLabel;

  const AppSvgIcon(
    this.svg, {
    super.key,
    this.size = 24.0,
    this.color,
    this.semanticsLabel,
  });

  @override
  Widget build(BuildContext context) {
    final effectiveColor = color ?? IconTheme.of(context).color ?? Colors.white;

    return SvgPicture.string(
      svg,
      width: size,
      height: size,
      fit: BoxFit.contain,
      colorFilter: ColorFilter.mode(effectiveColor, BlendMode.srcIn),
      semanticsLabel: semanticsLabel,
    );
  }
}
