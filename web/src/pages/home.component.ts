import { Component, signal, inject, OnInit, OnDestroy, ChangeDetectionStrategy, NgZone, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IconComponent } from '../components/icon.component';
import { PlatformService, PlatformInfo } from '../services/platform.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Product+Sans:wght@400;700&display=swap');

    .product-sans {
      font-family: 'Product Sans', system-ui, -apple-system, sans-serif;
    }

    /* Reduced motion support */
    @media (prefers-reduced-motion: reduce) {
      *, *::before, *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
      }
    }

    /* Content visibility for off-screen sections */
    .section-lazy {
      content-visibility: auto;
      contain-intrinsic-size: 0 600px;
    }

    /* Optimized gradient animation */
    .animate-gradient {
      background-size: 200% auto;
      animation: gradient 4s linear infinite;
    }
    @keyframes gradient {
      0% { background-position: 0% center; }
      100% { background-position: 200% center; }
    }

    /* Typing cursor */
    .cursor::after {
      content: '|';
      animation: blink 1s step-end infinite;
      color: currentColor;
      margin-left: 2px;
    }
    @keyframes blink {
      0%, 50% { opacity: 1; }
      51%, 100% { opacity: 0; }
    }

    /* Smooth float animation - GPU accelerated */
    .float {
      animation: float 6s ease-in-out infinite;
      will-change: transform;
    }
    @keyframes float {
      0%, 100% { transform: translateY(0) translateZ(0); }
      50% { transform: translateY(-10px) translateZ(0); }
    }

    /* Glow effect - simplified */
    .glow-indigo {
      box-shadow: 0 0 60px -15px rgba(99, 102, 241, 0.4);
    }
    :host-context(.dark) .glow-indigo {
      box-shadow: 0 0 80px -15px rgba(99, 102, 241, 0.3);
    }

    /* Gradient border button with animated glow */
    .btn-gradient {
      position: relative;
      background: linear-gradient(90deg, #6366f1, #8b5cf6, #d946ef, #ec4899, #6366f1);
      background-size: 200% 100%;
      animation: gradientFlow 2s linear infinite;
      box-shadow: 0 0 20px rgba(139, 92, 246, 0.5), 0 0 40px rgba(139, 92, 246, 0.2);
      transition: all 0.3s ease;
      overflow: hidden;
    }
    .btn-gradient:hover {
      box-shadow: 0 0 30px rgba(139, 92, 246, 0.7), 0 0 60px rgba(139, 92, 246, 0.4);
      transform: translateY(-3px) scale(1.02);
    }
    .btn-gradient:active {
      transform: translateY(0) scale(0.98);
    }
    @keyframes gradientFlow {
      0% { background-position: 0% 50%; }
      100% { background-position: 200% 50%; }
    }
    .btn-gradient::before {
      content: '';
      position: absolute;
      inset: 2px;
      background: white;
      border-radius: inherit;
      z-index: 0;
    }
    :host-context(.dark) .btn-gradient::before {
      background: #0f172a;
    }
    .btn-gradient::after {
      content: '';
      position: absolute;
      top: -50%;
      left: -50%;
      width: 200%;
      height: 200%;
      background: linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.3) 50%, transparent 70%);
      animation: shimmer 3s ease-in-out infinite;
      z-index: 2;
      pointer-events: none;
    }
    @keyframes shimmer {
      0% { transform: translateX(-100%) rotate(0deg); }
      100% { transform: translateX(100%) rotate(0deg); }
    }
    .btn-gradient > * {
      position: relative;
      z-index: 3;
    }
  `],
  template: `
    <div class="min-h-screen bg-white dark:bg-slate-950">
      <!-- Hero Section -->
      <section class="relative pt-28 pb-16 lg:pt-40 lg:pb-24 overflow-hidden">
        <!-- Background - Simplified gradients -->
        <div class="absolute inset-0 -z-10">
          <div class="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-gradient-to-b from-indigo-500/20 to-transparent rounded-full blur-3xl"></div>
          <div class="absolute bottom-0 right-0 w-[500px] h-[400px] bg-gradient-to-t from-cyan-500/10 to-transparent rounded-full blur-3xl"></div>
        </div>

        <div class="container mx-auto px-6 text-center">
          <!-- Status Badge -->
          <div class="flex justify-center mb-8">
            <a routerLink="/changelog" 
               class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 transition-colors">
              <span class="relative flex h-2 w-2">
                <span class="absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75 animate-ping"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
              </span>
              <span class="text-sm font-medium text-indigo-700 dark:text-indigo-300">v1.0 is Live</span>
              <svg class="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </a>
          </div>

          <!-- Headline -->
          <h1 class="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight mb-6 text-slate-900 dark:text-white">
            The Social Network<br/>
            <span class="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 via-purple-600 to-cyan-500 dark:from-indigo-400 dark:via-purple-400 dark:to-cyan-400 animate-gradient">
              You Own
            </span>
          </h1>

          <!-- Subheadline with typing effect -->
          <p class="text-lg md:text-xl max-w-2xl mx-auto mb-10 text-slate-600 dark:text-slate-400 min-h-[3.5rem]">
            <span class="cursor">{{ typedText() }}</span>
          </p>

          <!-- CTA Buttons -->
          <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
            <a [href]="getDownloadLink()" 
               class="w-full sm:w-auto px-8 py-4 rounded-xl font-semibold bg-slate-900 text-white hover:bg-slate-800 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-100 transition-colors shadow-lg flex items-center justify-center gap-2">
              <app-icon name="download" [size]="20"></app-icon>
              {{ getDownloadText() }}
            </a>

            <a routerLink="/app" 
               class="w-full sm:w-auto px-8 py-4 rounded-xl font-semibold btn-gradient">
              <span class="relative z-10 flex items-center justify-center gap-2 text-slate-900 dark:text-white">
                <app-icon name="zap" [size]="20"></app-icon>
                Launch Web App
              </span>
            </a>
          </div>


        </div>
      </section>

      <!-- Features Section - Stacking Cards -->
      <section class="relative bg-slate-50 dark:bg-slate-900/50" #featuresSection>
        <div class="sticky top-0 min-h-screen flex flex-col items-center justify-center py-20">
          <div class="text-center mb-12 px-6">
            <h2 class="text-3xl md:text-4xl font-bold text-slate-900 dark:text-white mb-4">
              Your Data. Your Rules.
            </h2>
            <p class="text-lg text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
              Built for privacy, designed for speed, made for everyone.
            </p>
          </div>

          <div class="relative w-full max-w-md mx-auto px-6 h-[220px] overflow-visible" style="perspective: 1000px">
            @for (feature of features; track feature.title; let i = $index) {
              <div class="absolute inset-x-6 top-0 will-change-transform"
                   [style.transform]="getCardTransform(i)"
                   [style.opacity]="getCardOpacity(i)"
                   [style.z-index]="getCardZIndex(i)"
                   [style.filter]="getCardBlur(i)"
                   [style.transition]="'all ' + (600 + i * 50) + 'ms cubic-bezier(0.34,1.56,0.64,1)'">
                <div class="p-6 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 transition-shadow duration-500"
                     [class.shadow-2xl]="activeCardIndex() === i"
                     [class.shadow-lg]="activeCardIndex() !== i">
                  <div class="w-12 h-12 rounded-xl mb-4 flex items-center justify-center" [class]="feature.bgClass">
                    <app-icon [name]="feature.icon" [size]="24" [class]="feature.iconClass"></app-icon>
                  </div>
                  <h3 class="text-lg font-semibold text-slate-900 dark:text-white mb-2">{{ feature.title }}</h3>
                  <p class="text-slate-600 dark:text-slate-400 text-sm leading-relaxed">{{ feature.description }}</p>
                </div>
              </div>
            }
          </div>
          
          <div class="mt-8 flex gap-2">
            @for (feature of features; track feature.title; let i = $index) {
              <button (click)="activeCardIndex.set(i)" 
                      class="h-2 rounded-full transition-all duration-300"
                      [ngClass]="activeCardIndex() === i ? 'w-4 bg-indigo-500' : 'w-2 bg-slate-300 dark:bg-slate-600'"></button>
            }
          </div>
        </div>
        <div [style.height.px]="features.length * 300"></div>
      </section>

      <!-- Code Preview Section -->
      <section class="py-20 section-lazy bg-white dark:bg-slate-950">
        <div class="container mx-auto px-6">
          <div class="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <h2 class="product-sans text-3xl md:text-4xl font-bold text-slate-900 dark:text-white mb-6">
                Open Source & Transparent
              </h2>
              <p class="text-lg text-slate-600 dark:text-slate-400 mb-8 leading-relaxed">
                Fully open source. Audit the code, contribute features, and help shape the future of social media.
              </p>
              <div class="flex flex-wrap gap-4">
                <a href="https://github.com/SynapseOSS/core" target="_blank" rel="noopener"
                   class="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 font-medium hover:opacity-90 transition-opacity">
                  <app-icon name="code" [size]="20"></app-icon>
                  View on GitHub
                </a>
                <a routerLink="/docs"
                   class="inline-flex items-center gap-2 px-6 py-3 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-medium hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
                  Read Docs
                </a>
              </div>
            </div>

            <div class="rounded-2xl bg-slate-900 dark:bg-slate-950 border border-slate-800 p-6 shadow-2xl">
              <div class="flex items-center gap-2 mb-4 pb-4 border-b border-slate-800">
                <div class="flex gap-1.5">
                  <div class="w-3 h-3 rounded-full bg-red-500"></div>
                  <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
                  <div class="w-3 h-3 rounded-full bg-green-500"></div>
                </div>
                <span class="text-xs text-slate-500 font-mono ml-2">terminal</span>
              </div>
              <div class="font-mono text-sm space-y-2">
                <p><span class="text-pink-400">$</span> <span class="text-white">git clone https://github.com/SynapseOSS/core</span></p>
                <p><span class="text-pink-400">$</span> <span class="text-white">cd core && npm install</span></p>
                <p><span class="text-pink-400">$</span> <span class="text-white">npm run dev</span></p>
                <p class="text-green-400">✓ Server running on localhost:3000</p>
                <p class="text-slate-500">Ready to build the future of social_</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Newsletter Section -->
      <section class="py-20 section-lazy bg-slate-50 dark:bg-slate-900/50">
        <div class="container mx-auto px-6">
          <div class="max-w-2xl mx-auto text-center">
            <div class="inline-flex items-center gap-2 mb-4">
              <span class="w-2 h-2 rounded-full bg-red-500 animate-pulse"></span>
              <span class="text-sm font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider">Live Updates</span>
            </div>
            
            <h2 class="text-3xl md:text-4xl font-bold text-slate-900 dark:text-white mb-4">
              The signal, <span class="text-indigo-600 dark:text-indigo-400">minus the noise.</span>
            </h2>
            
            <p class="text-lg text-slate-600 dark:text-slate-400 mb-8">
              Get the weekly digest of decentralized tech news and community highlights.
            </p>

            <form class="flex flex-col sm:flex-row gap-3 max-w-md mx-auto" (submit)="$event.preventDefault()">
              <input type="email" placeholder="you@example.com" 
                     class="flex-1 px-4 py-3 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
              <button type="submit" 
                      class="px-6 py-3 rounded-xl font-semibold bg-indigo-600 text-white hover:bg-indigo-500 transition-colors shadow-lg shadow-indigo-500/25">
                Subscribe
              </button>
            </form>
            
            <p class="text-sm text-slate-500 mt-4">Join 45,000+ developers. No spam, unsubscribe anytime.</p>
          </div>
        </div>
      </section>
    </div>
  `
})
export class HomeComponent implements OnInit, OnDestroy, AfterViewInit {
  private platformService = inject(PlatformService);
  private ngZone = inject(NgZone);
  
  @ViewChild('featuresSection') featuresSection!: ElementRef<HTMLElement>;
  
  typedText = signal('');
  activeCardIndex = signal(0);
  scrollProgress = signal(0);
  private typingTimeout: any;
  private currentTextIndex = 0;
  private currentCharIndex = 0;
  private isDeleting = false;
  private scrollHandler: (() => void) | null = null;
  
  private readonly texts = [
    'No ads. No tracking. No corporate control.',
    'Your data, your rules, your network.',
    'Connect freely, share securely.',
    'Built by the community, for the community.'
  ];

  readonly features = [
    {
      icon: 'lock',
      title: 'End-to-End Encryption',
      description: 'Your messages are encrypted. Only you and your recipients can read them.',
      bgClass: 'bg-indigo-100 dark:bg-indigo-900/30',
      iconClass: 'text-indigo-600 dark:text-indigo-400',
      direction: 'bottom' as const
    },
    {
      icon: 'database',
      title: 'Own Your Data',
      description: 'Connect your own storage. Your content stays on your infrastructure.',
      bgClass: 'bg-cyan-100 dark:bg-cyan-900/30',
      iconClass: 'text-cyan-600 dark:text-cyan-400',
      direction: 'left' as const
    },
    {
      icon: 'sparkles',
      title: 'AI-Powered',
      description: 'Smart suggestions and search that respects your privacy.',
      bgClass: 'bg-purple-100 dark:bg-purple-900/30',
      iconClass: 'text-purple-600 dark:text-purple-400',
      direction: 'right' as const
    },
    {
      icon: 'code',
      title: 'Open Source',
      description: 'Fully transparent. Audit, contribute, and shape the future.',
      bgClass: 'bg-green-100 dark:bg-green-900/30',
      iconClass: 'text-green-600 dark:text-green-400',
      direction: 'bottom' as const
    }
  ];

  ngOnInit() {
    // Run typing animation outside Angular zone for better performance
    this.ngZone.runOutsideAngular(() => {
      setTimeout(() => this.typeText(), 500);
    });
  }

  ngAfterViewInit() {
    if (typeof window !== 'undefined') {
      this.scrollHandler = () => this.onScroll();
      window.addEventListener('scroll', this.scrollHandler, { passive: true });
    }
  }

  ngOnDestroy() {
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }
    if (this.scrollHandler) {
      window.removeEventListener('scroll', this.scrollHandler);
    }
  }

  private onScroll() {
    if (!this.featuresSection?.nativeElement) return;
    
    const section = this.featuresSection.nativeElement;
    const rect = section.getBoundingClientRect();
    const sectionHeight = section.offsetHeight;
    const scrollableHeight = sectionHeight - window.innerHeight;
    
    if (rect.top <= 0 && rect.bottom >= window.innerHeight) {
      const progress = Math.abs(rect.top) / scrollableHeight;
      const cardIndex = Math.min(Math.floor(progress * this.features.length), this.features.length - 1);
      
      this.ngZone.run(() => {
        this.scrollProgress.set(progress);
        this.activeCardIndex.set(cardIndex);
      });
    }
  }

  getCardTransform(index: number): string {
    const activeIndex = this.activeCardIndex();
    const diff = index - activeIndex;
    const direction = this.features[index].direction;
    
    if (diff < 0) {
      // Passed cards - stack with 3D tilt
      const stackOffset = diff * 6;
      const scale = 1 + (diff * 0.015);
      const rotateZ = diff * 1.5;
      const rotateX = diff * -2;
      return `translateY(${stackOffset}px) translateZ(${diff * 20}px) scale(${Math.max(scale, 0.94)}) rotateZ(${rotateZ}deg) rotateX(${rotateX}deg)`;
    } else if (diff === 0) {
      return 'translateY(0) translateZ(0) scale(1) rotateZ(0deg) rotateX(0deg)';
    } else {
      // Incoming cards with 3D depth
      const distance = 100 + diff * 40;
      const scale = 0.85 - (diff * 0.05);
      const rotateZ = diff * 4;
      const translateZ = -50 * diff;
      
      switch (direction) {
        case 'left':
          return `translateX(-${distance}px) translateY(30px) translateZ(${translateZ}px) scale(${scale}) rotateZ(-${rotateZ}deg) rotateY(15deg)`;
        case 'right':
          return `translateX(${distance}px) translateY(30px) translateZ(${translateZ}px) scale(${scale}) rotateZ(${rotateZ}deg) rotateY(-15deg)`;
        default:
          return `translateY(${distance}px) translateZ(${translateZ}px) scale(${scale}) rotateX(-10deg)`;
      }
    }
  }

  getCardBlur(index: number): string {
    const diff = index - this.activeCardIndex();
    if (diff === 0) return 'blur(0px)';
    if (diff < 0) return `blur(${Math.min(Math.abs(diff) * 0.5, 2)}px)`;
    return `blur(${Math.min(diff * 2, 6)}px)`;
  }

  getCardOpacity(index: number): number {
    const diff = index - this.activeCardIndex();
    
    if (diff < -2) return 0;
    if (diff < 0) return 0.4 + (diff * 0.1);
    if (diff === 0) return 1;
    if (diff === 1) return 0.5;
    if (diff === 2) return 0.2;
    return 0;
  }

  getCardZIndex(index: number): number {
    const activeIndex = this.activeCardIndex();
    const diff = index - activeIndex;
    
    if (diff <= 0) {
      // Active and passed cards - higher z-index for more recent
      return this.features.length + diff;
    }
    // Future cards - below
    return 0;
  }

  private typeText() {
    const currentText = this.texts[this.currentTextIndex];
    const typeSpeed = this.isDeleting ? 25 : 50;
    const pauseTime = 2500;

    if (!this.isDeleting) {
      if (this.currentCharIndex < currentText.length) {
        this.currentCharIndex++;
        this.ngZone.run(() => {
          this.typedText.set(currentText.substring(0, this.currentCharIndex));
        });
        this.typingTimeout = setTimeout(() => this.typeText(), typeSpeed);
      } else {
        this.typingTimeout = setTimeout(() => {
          this.isDeleting = true;
          this.typeText();
        }, pauseTime);
      }
    } else {
      if (this.currentCharIndex > 0) {
        this.currentCharIndex--;
        this.ngZone.run(() => {
          this.typedText.set(currentText.substring(0, this.currentCharIndex));
        });
        this.typingTimeout = setTimeout(() => this.typeText(), typeSpeed);
      } else {
        this.isDeleting = false;
        this.currentTextIndex = (this.currentTextIndex + 1) % this.texts.length;
        this.typingTimeout = setTimeout(() => this.typeText(), 300);
      }
    }
  }

  getDownloadLink(): string {
    const platforms = this.platformService.getRecommendedPlatforms();
    return platforms.length > 0 && platforms[0].action === 'Install PWA' ? '/app' : '/downloads';
  }

  getDownloadText(): string {
    if (typeof navigator === 'undefined') return 'Download App';
    const ua = navigator.userAgent.toLowerCase();
    if (ua.includes('android')) return 'Get for Android';
    if (ua.includes('iphone') || ua.includes('ipad')) return 'Get for iOS';
    if (ua.includes('mac')) return 'Get for Mac';
    if (ua.includes('windows')) return 'Get for Windows';
    return 'Download App';
  }
}
