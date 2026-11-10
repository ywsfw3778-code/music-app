import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../providers/music_provider.dart';
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
        return Scaffold(
          backgroundColor: AppColors.background,
          body: Stack(
            children: [
              // Ambient Liquid Glowing Mesh (Liquid Glass visual foundation)
              Positioned(
                top: -60,
                right: -60,
                child: IgnorePointer(
                  child: Container(
                    width: 340,
                    height: 340,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          AppColors.ambientOrb1.withValues(alpha: 0.22),
                          AppColors.ambientOrb1.withValues(alpha: 0.0),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              Positioned(
                bottom: 80,
                left: -80,
                child: IgnorePointer(
                  child: Container(
                    width: 360,
                    height: 360,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          AppColors.ambientOrb2.withValues(alpha: 0.20),
                          AppColors.ambientOrb2.withValues(alpha: 0.0),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              Positioned(
                top: MediaQuery.of(context).size.height * 0.45,
                right: -80,
                child: IgnorePointer(
                  child: Container(
                    width: 260,
                    height: 260,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          AppColors.accent.withValues(alpha: 0.12),
                          AppColors.accent.withValues(alpha: 0.0),
                        ],
                      ),
                    ),
                  ),
                ),
              ),

              // Smooth PageView
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

              // Floating Player + Liquid Glass Bottom Dock
              Positioned(
                left: 0,
                right: 0,
                bottom: 0,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Floating Mini Player with Liquid Glass
                    const FloatingMiniPlayer(),

                    // Liquid Glass Hold & Drag Dock
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
                                      color: AppColors.primary.withValues(alpha: _isDragging ? 0.35 : 0.14),
                                      blurRadius: _isDragging ? 28 : 20,
                                      offset: const Offset(0, 8),
                                    ),
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.65),
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
                                        color: Colors.white.withValues(alpha: 0.08),
                                        borderRadius: BorderRadius.circular(36),
                                        border: Border.all(
                                          color: _isDragging
                                              ? AppColors.primary.withValues(alpha: 0.7)
                                              : Colors.white.withValues(alpha: 0.22),
                                          width: 1.3,
                                        ),
                                        gradient: LinearGradient(
                                          begin: Alignment.topLeft,
                                          end: Alignment.bottomRight,
                                          colors: [
                                            Colors.white.withValues(alpha: 0.16),
                                            AppColors.surface.withValues(alpha: 0.70),
                                          ],
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
                                                gradient: LinearGradient(
                                                  colors: AppColors.primaryGradient,
                                                  begin: Alignment.topLeft,
                                                  end: Alignment.bottomRight,
                                                ),
                                                boxShadow: [
                                                  BoxShadow(
                                                    color: AppColors.primary.withValues(alpha: 0.45),
                                                    blurRadius: 14,
                                                    offset: const Offset(0, 3),
                                                  ),
                                                ],
                                              ),
                                            ),
                                          ),

                                          // The 3 Tabs
                                          Row(
                                            children: [
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 0,
                                                  icon: Icons.home_rounded,
                                                  label: 'الرئيسية',
                                                ),
                                              ),
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 1,
                                                  icon: Icons.music_note_rounded,
                                                  label: 'الأغاني',
                                                ),
                                              ),
                                              Expanded(
                                                child: _buildTabItem(
                                                  index: 2,
                                                  icon: Icons.queue_music_rounded,
                                                  label: 'القوائم',
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
    required IconData icon,
    required String label,
  }) {
    final isSelected = _currentIndex == index;

    return SizedBox(
      height: 48,
      child: Center(
        child: AnimatedDefaultTextStyle(
          duration: const Duration(milliseconds: 200),
          style: TextStyle(
            color: isSelected ? AppColors.onPrimary : Colors.white.withValues(alpha: 0.7),
            fontWeight: isSelected ? FontWeight.w900 : FontWeight.w600,
            fontSize: 12,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                color: isSelected ? AppColors.onPrimary : Colors.white.withValues(alpha: 0.7),
                size: 18,
              ),
              if (isSelected) ...[
                const SizedBox(width: 4),
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
    );
  }
}
