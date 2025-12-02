
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { IconComponent } from './icon.component';
import { LogoComponent } from './logo.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [IconComponent, RouterModule, LogoComponent],
  template: `
    <footer class="relative z-10 overflow-hidden bg-slate-50 dark:bg-slate-900">
      <!-- Gradient mesh background -->
      <div class="absolute inset-0 opacity-20 dark:opacity-30">
        <div class="absolute top-0 left-1/4 w-96 h-96 bg-indigo-600 rounded-full blur-[128px]"></div>
        <div class="absolute bottom-0 right-1/4 w-96 h-96 bg-purple-600 rounded-full blur-[128px]"></div>
      </div>
      
      <div class="relative container mx-auto px-6 py-10">
        <!-- Main content - single row on desktop -->
        <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-8 mb-8">
          
          <!-- Brand -->
          <div class="flex-shrink-0">
            <div class="flex items-center gap-2 mb-3 group cursor-pointer">
              <app-logo [size]="32"></app-logo>
              <span class="text-xl font-bold text-slate-900 dark:text-white">Synapse</span>
            </div>
            <p class="text-slate-600 dark:text-slate-400 text-sm max-w-xs mb-4">Open source social, built for people.</p>
            
            <!-- Social icons -->
            <div class="flex gap-2">
              @for (social of socials; track social.name) {
                <a [href]="social.url" target="_blank" [attr.aria-label]="social.name"
                   class="w-9 h-9 rounded-lg bg-slate-200 dark:bg-white/5 hover:bg-slate-300 dark:hover:bg-white/10 flex items-center justify-center text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-all hover:scale-110 hover:-translate-y-0.5">
                  <app-icon [name]="social.icon" [size]="16"></app-icon>
                </a>
              }
            </div>
          </div>

          <!-- Links - horizontal on desktop -->
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-x-12 gap-y-6 text-sm">
            @for (section of links; track section.title) {
              <div>
                <h4 class="font-semibold text-slate-900 dark:text-white mb-3">{{ section.title }}</h4>
                <ul class="space-y-2">
                  @for (link of section.items; track link.label) {
                    <li>
                      @if (link.route) {
                        <a [routerLink]="link.route" [fragment]="link.fragment" class="text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors">{{ link.label }}</a>
                      } @else {
                        <a [href]="link.href" class="text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors">{{ link.label }}</a>
                      }
                    </li>
                  }
                </ul>
              </div>
            }
          </div>
        </div>

        <!-- Bottom bar -->
        <div class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-6 border-t border-slate-200 dark:border-white/10">
          <span class="text-slate-500 dark:text-slate-500 text-sm">&copy; {{ year }} Synapse Foundation</span>
          <div class="flex items-center gap-2 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
            <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-500 dark:bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500 dark:bg-emerald-400"></span>
            </span>
            All systems operational
          </div>
        </div>
      </div>
    </footer>
  `
})
export class FooterComponent {
  year = new Date().getFullYear();
  
  socials = [
    { name: 'Twitter', icon: 'twitter', url: 'https://twitter.com' },
    { name: 'Discord', icon: 'discord', url: 'https://discord.gg' },
    { name: 'GitHub', icon: 'github', url: 'https://github.com/SynapseOSS' },
    { name: 'LinkedIn', icon: 'linkedin', url: 'https://linkedin.com' }
  ];
  
  links = [
    {
      title: 'Product',
      items: [
        { label: 'Features', route: '/', fragment: 'features' },
        { label: 'Pricing', route: '/pricing' },
        { label: 'Changelog', route: '/changelog' },
        { label: 'Roadmap', route: '/roadmap' }
      ]
    },
    {
      title: 'Resources',
      items: [
        { label: 'Documentation', route: '/docs' },
        { label: 'API Reference', route: '/docs/api' },
        { label: 'Help Center', route: '/support' }
      ]
    },
    {
      title: 'Company',
      items: [
        { label: 'About', route: '/about' },
        { label: 'Contact', href: 'mailto:hello@synapse.social' },
        { label: 'Privacy', route: '/docs/privacy' },
        { label: 'Terms', route: '/docs/terms' }
      ]
    }
  ];
}
