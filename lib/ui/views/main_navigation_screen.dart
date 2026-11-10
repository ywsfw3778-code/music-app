import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_svg_icons.dart';
import '../../core/theme/app_theme.dart';
import '../../providers/music_provider.dart';
import '../widgets/app_svg_icon.dart';
import '../widgets/floating_mini_player.dart';
import 'all_songs_view.dart';
import 'home_view.dart';
import 'playlists_view.dart';

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  int _currentIndex = 0;
  late final PageController _pageController;
  bool _isDragging = false;

  final List<Widget> _screens = const [
    HomeView(),
    AllSongsView(),
    PlaylistsView(),
  ];

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: _currentIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onTabTapped(int index) {
    if (index == _currentIndex) return;
    setState(() => _currentIndex = index);
    HapticFeedback.selectionClick();
    _pageController.animateToPage(
      index,
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  void _handleDragUpdate(Offset localPosition, double totalWidth, bool isRtl) {
    if (totalWidth <= 0) return;
    final clampedX = localPosition.dx.clamp(0.0, totalWidth - 1);
    final tabWidth = totalWidth / 3;
    final rawIndex = (clampedX / tabWidth).floor().clamp(0, 2);
    final targetIndex = isRtl ? (2 - rawIndex) : rawIndex;

    if (targetIndex != _currentIndex) {
      _onTabTapped(targetIndex);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isRtl = Directionality.of(context) == TextDirection.rtl;

    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        final song = provider.currentSong;
        final accentColor = DynamicPalette.getColor(song);

        return Scaffold(
          backgroundColor: AppColors.amoledBlack,
          body: Stack(
            children: [
              // Clean PageView over Pure OLED Black (#000000)
              PageView(
                controller: _pageController,
                physics: const BouncingScrollPhysics(),
                onPageChanged: (index) {
                  if (_currentIndex != index) {
                    setState(() => _currentIndex = index);
                    HapticFeedback.selectionClick();
                  }
                },
                children: _screens,
              ),

              // Floating Player + Minimalist AMOLED Dock
              Positioned(
                left: 0,
                right: 0,
                bottom: 0,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Floating Mini Player
                    const FloatingMiniPlayer(),

                    // Minimalist AMOLED Dock
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          final totalWidth = constraints.maxWidth;
                          final tabWidth = (totalWidth - 12) / 3;

                          return GestureDetector(
                            onPanDown: (details) {
                              setState(() => _isDragging = true);
                              _handleDragUpdate(details.localPosition, totalWidth, isRtl);
                            },
                            onPanUpdate: (details) {
                              _handleDragUpdate(details.localPosition, totalWidth, isRtl);
                            },
                            onPanEnd: (_) => setState(() => _isDragging = false),
                            onPanCancel: () => setState(() => _isDragging = false),
                            child: AnimatedScale(
                              scale: _isDragging ? 1.02 : 1.0,
                              duration: const Duration(milliseconds: 180),
                              curve: Curves.easeOutCubic,
                              child: Container(
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(36),
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.85),
                                      blurRadius: 26,
                                      offset: const Offset(0, 10),
                                    ),
                                  ],
                                ),
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(36),
                                  child: BackdropFilter(
                                    filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
                                    child: Container(
                                      padding: const EdgeInsets.all(6),
                                      decoration: BoxDecoration(
                                        color: AppColors.surface.withValues(alpha: 0.90),
                                        borderRadius: BorderRadius.circular(36),
                                        border: Border.all(
                                          color: Colors.white.withValues(alpha: 0.12),
                                          width: 1.0,
                                        ),
                                      ),
                                      child: Stack(
                                        children: [
                                          // Fluid Sliding Capsule Indicator
                                          AnimatedPositioned(
                                            duration: const Duration(milliseconds: 280),
                                            curve: Curves.easeOutBack,
                                            left: isRtl
                                                ? null
                                                : _currentIndex * tabWidth,
                                            right: isRtl
                                                ? _currentIndex * tabWidth
                                                : null,
                                            top: 0,
                                            bottom: 0,
                                            width: tabWidth,
                                            child: Container(
                                              decoration: BoxDecoration(
                                                borderRadius: BorderRadius.circular(28),
                                                color: accentColor.withValues(alpha: 0.20),
                                                border: Border.all(
                                                  color: accentColor.withValues(alpha: 0.45),
                                                  width: 1.2,
                                                ),
                                              ),
                                            ),
                                          ),

                                          // The 3 Tabs using Custom SVG Vector Paths
                                          Row(
                                            children: [
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 0,
                                                  svgIcon: AppSvgIcons.home,
                                                  label: 'الرئيسية',
                                                  accentColor: accentColor,
                                                ),
                                              ),
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 1,
                                                  svgIcon: AppSvgIcons.musicNote,
                                                  label: 'الأغاني',
                                                  accentColor: accentColor,
                                                ),
                                              ),
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 2,
                                                  svgIcon: AppSvgIcons.playlist,
                                                  label: 'القوائم',
                                                  accentColor: accentColor,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildTabItem({
    required int index,
    required String svgIcon,
    required String label,
    required Color accentColor,
  }) {
    final isSelected = _currentIndex == index;

    return GestureDetector(
      onTap: () => _onTabTapped(index),
      behavior: HitTestBehavior.opaque,
      child: SizedBox(
        height: 48,
        child: Center(
          child: AnimatedDefaultTextStyle(
            duration: const Duration(milliseconds: 200),
            style: TextStyle(
              color: isSelected ? accentColor : Colors.white.withValues(alpha: 0.7),
              fontWeight: isSelected ? FontWeight.w800 : FontWeight.w500,
              fontSize: 12,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                AppSvgIcon(
                  svgIcon,
                  color: isSelected ? accentColor : Colors.white.withValues(alpha: 0.7),
                  size: 20,
                ),
                if (isSelected) ...[
                  const SizedBox(width: 6),
                  Flexible(
                    child: Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
