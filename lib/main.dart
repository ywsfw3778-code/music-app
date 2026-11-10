import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'core/constants/app_colors.dart';
import 'core/theme/app_theme.dart';
import 'providers/music_provider.dart';
import 'services/storage_service.dart';
import 'ui/views/main_navigation_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Dark navigation bar and status bar
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );

  // Initialize persistent storage
  final storageService = await StorageService.init();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => MusicProvider(storageService),
        ),
      ],
      child: const EchoMusicApp(),
    ),
  );
}

class EchoMusicApp extends StatelessWidget {
  const EchoMusicApp({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        return MaterialApp(
          title: 'Echo Music',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.darkTheme,
          home: const Directionality(
            textDirection: TextDirection.rtl,
            child: WebPhoneFrame(
              child: MainNavigationScreen(),
            ),
          ),
        );
      },
    );
  }
}

class WebPhoneFrame extends StatelessWidget {
  final Widget child;
  const WebPhoneFrame({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth > 650) {
          return Scaffold(
            backgroundColor: const Color(0xFF040407),
            body: Stack(
              alignment: Alignment.center,
              children: [
                // Ambient Aurora backdrop behind the phone
                Positioned(
                  top: -80,
                  left: -80,
                  child: Container(
                    width: 500,
                    height: 500,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          AppColors.ambientOrb1.withValues(alpha: 0.16),
                          Colors.transparent,
                        ],
                      ),
                    ),
                  ),
                ),
                Positioned(
                  bottom: -80,
                  right: -80,
                  child: Container(
                    width: 500,
                    height: 500,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          AppColors.ambientOrb2.withValues(alpha: 0.14),
                          Colors.transparent,
                        ],
                      ),
                    ),
                  ),
                ),

                // Sleek Centered Phone Mockup with Glass Reflections
                Center(
                  child: Container(
                    width: 420,
                    height: (constraints.maxHeight * 0.94).clamp(650.0, 880.0),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(44),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.22),
                        width: 2.0,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.primary.withValues(alpha: 0.25),
                          blurRadius: 40,
                          spreadRadius: 2,
                        ),
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.85),
                          blurRadius: 60,
                          offset: const Offset(0, 20),
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(42),
                      child: child,
                    ),
                  ),
                ),
              ],
            ),
          );
        }
        return child;
      },
    );
  }
}
