import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IconComponent } from './icon.component';
import { LogoComponent } from './logo.component';
import { AuthService } from '../services/auth.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent, LogoComponent],
  styles: [`
    /* Animated hamburger to X */
    .hamburger {
      width: 24px;
      height: 24px;
      position: relative;
      cursor: pointer;
    }
    .hamburger span {
      display: block;
      position: absolute;
      height: 2px;
      width: 100%;
      background: currentColor;
      border-radius: 2px;
      left: 0;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .hamburger span:nth-child(1) { top: 6px; }
    .hamburger span:nth-child(2) { top: 11px; }
    .hamburger span:nth-child(3) { top: 16px; }
    
    .hamburger.open span:nth-child(1) {
      top: 11px;
      transform: rotate(45deg);
    }
    .hamburger.open span:nth-child(2) {
      opacity: 0;
      transform: translateX(-10px);
    }
    .hamburger.open span:nth-child(3) {
      top: 11px;
      transform: rotate(-45deg);
    }

    /* Mobile menu animation */
    .mobile-menu {
      animation: slideDown 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Menu item stagger animation */
    .menu-item {
      opacity: 0;
      animation: fadeInUp 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
    }
    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Accordion animation */
    .accordion-content {
      display: grid;
      grid-template-rows: 0fr;
      transition: grid-template-rows 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    .accordion-content.open {
      grid-template-rows: 1fr;
    }
    .accordion-inner {
      overflow: hidden;
    }
  `],
  template: `
    <nav class="fixed top-0 left-0 right-0 z-50 transition-all duration-300 py-3 bg-white/60 dark:bg-slate-900/60 backdrop-blur-xl"
         [class.shadow-lg]="isScrolled()"
         [class.shadow-slate-900/5]="isScrolled()"
         [class.border-b]="isScrolled()"
         [class.border-slate-200/50]="isScrolled()"
         [class.dark:border-white/10]="isScrolled()">
      
      <!-- Scroll Progress Bar -->
      <div class="absolute bottom-0 left-0 h-0.5 bg-gradient-to-r from-indigo-600 via-purple-500 to-cyan-500 transition-all duration-150"
           [style.width.%]="scrollPercent()"
           [class.opacity-0]="scrollPercent() < 1"></div>

      <div class="container mx-auto px-4 md:px-6 max-w-7xl">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <a routerLink="/" class="flex items-center gap-2 group">
            <app-logo [size]="36"></app-logo>
            <span class="text-xl font-bold text-slate-900 dark:text-white">Synapse</span>
          </a>

          <!-- Desktop Links -->
          <div class="hidden lg:flex items-center gap-1 absolute left-1/2 -translate-x-1/2">
            <button (mouseenter)="showMegaMenu.set(true)"
                    class="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors flex items-center gap-1">
              Products
              <app-icon name="chevron-down" [size]="16"></app-icon>
            </button>
          </div>

          <!-- Right Side -->
          <div class="flex items-center gap-3">
            <!-- Theme Toggle -->
            <button (click)="toggleTheme($event)" 
                    class="p-2 rounded-lg text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                    [attr.aria-label]="themeService.darkMode() ? 'Switch to light mode' : 'Switch to dark mode'">
              <app-icon [name]="themeService.darkMode() ? 'sun' : 'moon'" [size]="20"></app-icon>
            </button>

            @if (authService.currentUser()) {
              <!-- Admin access via direct link only -->
            } @else {
              <a routerLink="/login" class="hidden lg:block text-sm font-medium text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                Log In
              </a>
            }

            <a routerLink="/app" class="hidden lg:flex items-center gap-1.5 px-5 py-2 rounded-full text-sm font-semibold bg-slate-900 dark:bg-white text-white dark:text-slate-900 hover:bg-slate-800 dark:hover:bg-slate-100 transition-colors">
              Launch App
              <app-icon name="chevron-right" [size]="16"></app-icon>
            </a>
            
            <!-- Animated Hamburger Button -->
            <button class="lg:hidden p-2 text-slate-700 dark:text-slate-300" 
                    (click)="toggleMenu()"
                    aria-label="Toggle menu">
              <div class="hamburger" [class.open]="mobileMenuOpen()">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Mega Menu -->
      @if (showMegaMenu()) {
        <div class="hidden lg:block absolute top-full left-0 right-0 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 shadow-2xl"
             (mouseenter)="showMegaMenu.set(true)"
             (mouseleave)="showMegaMenu.set(false)">
          <div class="container mx-auto px-6 py-8 max-w-7xl">
            <div class="grid grid-cols-3 gap-8">
              @for (section of menuSections; track section.title) {
                <div>
                  <h3 class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-4">
                    {{ section.title }}
                  </h3>
                  <div class="space-y-1">
                    @for (item of section.items; track item.path) {
                      <a [href]="item.external ? item.path : null"
                         [routerLink]="!item.external ? item.path : null"
                         [target]="item.external ? '_blank' : null"
                         [rel]="item.external ? 'noopener' : null"
                         (click)="showMegaMenu.set(false)"
                         class="group flex items-start gap-3 p-3 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
                        <div class="mt-0.5 text-slate-400 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                          <app-icon [name]="item.icon" [size]="20" [strokeWidth]="1.5"></app-icon>
                        </div>
                        <div class="flex-1 min-w-0">
                          <div class="text-sm font-medium text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                            {{ item.label }}
                          </div>
                          <div class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                            {{ item.description }}
                          </div>
                        </div>
                      </a>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }

      <!-- Mobile Menu -->
      @if (mobileMenuOpen()) {
        <div class="lg:hidden mobile-menu absolute top-full left-0 right-0 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 shadow-xl">
          <div class="px-2 py-4">
            <div class="flex flex-col gap-1">
              @for (section of menuSections; track section.title) {
                <div class="border-b border-slate-200 dark:border-slate-800 last:border-0">
                  <button (click)="expandedSection.set(expandedSection() === section.title ? null : section.title)"
                          class="w-full flex items-center justify-between px-3 py-3 text-left transition-colors hover:bg-slate-50 dark:hover:bg-slate-800">
                    <h3 class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                      {{ section.title }}
                    </h3>
                    <div class="transition-transform duration-300" [style.transform]="expandedSection() === section.title ? 'rotate(180deg)' : 'rotate(0deg)'">
                      <app-icon name="chevron-down" [size]="16" class="text-slate-400"></app-icon>
                    </div>
                  </button>
                  <div class="accordion-content" [class.open]="expandedSection() === section.title">
                    <div class="accordion-inner">
                      <div class="pb-2 space-y-1">
                        @for (item of section.items; track item.path; let i = $index) {
                          <a [href]="item.external ? item.path : null"
                             [routerLink]="!item.external ? item.path : null"
                             [target]="item.external ? '_blank' : null"
                             (click)="toggleMenu()"
                             class="flex items-start gap-3 px-3 py-2.5 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all"
                             [style.animation-delay]="(i * 50) + 'ms'"
                             [style.opacity]="expandedSection() === section.title ? '1' : '0'"
                             [style.transform]="expandedSection() === section.title ? 'translateX(0)' : 'translateX(-10px)'"
                             [style.transition]="'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) ' + (i * 50) + 'ms'">
                            <app-icon [name]="item.icon" [size]="18" [strokeWidth]="1.5" class="text-slate-400 mt-0.5"></app-icon>
                            <div class="flex-1 min-w-0">
                              <div class="text-sm font-medium">{{ item.label }}</div>
                              <div class="text-xs text-slate-500 dark:text-slate-400">{{ item.description }}</div>
                            </div>
                          </a>
                        }
                      </div>
                    </div>
                  </div>
                </div>
              }
              
              <div class="h-px bg-slate-200 dark:bg-slate-800 my-2"></div>
              
              @if (authService.currentUser()) {
                <!-- Admin access via direct link only -->
              } @else {
                <a routerLink="/login" (click)="toggleMenu()" 
                   class="menu-item flex items-center gap-3 px-3 py-3 rounded-xl text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="log-in" [size]="20" class="text-slate-400"></app-icon>
                  <span class="font-medium">Log In</span>
                </a>
              }
              
              <a routerLink="/app" (click)="toggleMenu()" 
                 class="menu-item mt-2 flex items-center justify-center gap-2 px-3 py-3 rounded-xl bg-indigo-600 text-white font-semibold hover:bg-indigo-500 transition-colors">
                <app-icon name="zap" [size]="20"></app-icon>
                Launch App
              </a>
            </div>
          </div>
        </div>
      }
    </nav>
  `
})
export class NavbarComponent {
  authService = inject(AuthService);
  themeService = inject(ThemeService);
  isScrolled = signal(false);
  mobileMenuOpen = signal(false);
  scrollPercent = signal(0);

  readonly menuSections = [
    {
      title: 'Product',
      items: [
        { path: '/#features', label: 'Features', icon: 'sparkles', description: 'Explore what makes us different', strokeWidth: 1.5 },
        { path: '/pricing', label: 'Pricing', icon: 'credit-card', description: 'Simple, transparent pricing', strokeWidth: 1.5 },
        { path: '/roadmap', label: 'Roadmap', icon: 'map', description: 'See what we\'re building', strokeWidth: 1.5 },
        { path: '/changelog', label: 'Changelog', icon: 'git-commit', description: 'Latest updates & releases', strokeWidth: 1.5 },
      ]
    },
    {
      title: 'Resources',
      items: [
        { path: '/docs', label: 'Documentation', icon: 'book-open', description: 'Learn how to use Synapse', strokeWidth: 1.5 },
        { path: '/docs/api', label: 'API Reference', icon: 'code', description: 'Build with our API', strokeWidth: 1.5 },
        { path: '/support', label: 'Support', icon: 'help-circle', description: 'Get help when you need it', strokeWidth: 1.5 },
      ]
    },
    {
      title: 'Company',
      items: [
        { path: '/about', label: 'About', icon: 'users', description: 'Our mission and team', strokeWidth: 1.5 },
        { path: 'https://github.com/SynapseOSS', label: 'GitHub', icon: 'github', description: 'View source code', external: true, strokeWidth: 1.5 },
      ]
    }
  ];

  showMegaMenu = signal(false);
  expandedSection = signal<string | null>(null);

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('scroll', () => {
        this.isScrolled.set(window.scrollY > 20);
        const docHeight = document.documentElement.scrollHeight - window.innerHeight;
        this.scrollPercent.set(docHeight > 0 ? (window.scrollY / docHeight) * 100 : 0);
      }, { passive: true });
    }
  }

  toggleMenu() {
    this.mobileMenuOpen.update(v => !v);
    if (typeof document !== 'undefined') {
      document.body.style.overflow = this.mobileMenuOpen() ? 'hidden' : '';
    }
  }

  toggleTheme(event: MouseEvent) {
    this.themeService.toggle(event);
  }
}
