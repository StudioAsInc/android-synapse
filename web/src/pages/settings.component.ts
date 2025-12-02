import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IconComponent } from '../components/icon.component';
import { ImageUploadService, ImageProvider, ProviderConfig, FileType } from '../services/image-upload.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, IconComponent, FormsModule],
  template: `
    <div class="min-h-screen bg-slate-50 dark:bg-slate-950">
      <!-- Header -->
      <header class="sticky top-0 z-30 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800">
        <div class="max-w-5xl mx-auto px-4 h-14 flex items-center gap-3">
          <button (click)="goBack()" class="p-2 -ml-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 lg:hidden">
            <app-icon name="arrow-left" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
          </button>
          <h1 class="text-lg font-semibold text-slate-900 dark:text-white">Settings</h1>
        </div>
      </header>

      <div class="max-w-5xl mx-auto lg:flex">
        <!-- Sidebar Navigation -->
        <nav class="hidden lg:block w-60 shrink-0 p-4 sticky top-14 h-[calc(100vh-3.5rem)] overflow-y-auto">
          <div class="space-y-1">
            @for (section of sections; track section.id) {
              <button
                (click)="activeSection.set(section.id)"
                [class]="activeSection() === section.id 
                  ? 'bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-white' 
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'"
                class="w-full px-3 py-2.5 rounded-lg flex items-center gap-3 text-sm font-medium transition-colors">
                <app-icon [name]="section.icon" [size]="18"></app-icon>
                {{ section.label }}
              </button>
            }
          </div>
        </nav>

        <!-- Mobile Navigation -->
        <div class="lg:hidden border-b border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 sticky top-14 z-20 overflow-x-auto">
          <div class="flex px-2 gap-1">
            @for (section of sections; track section.id) {
              <button
                (click)="activeSection.set(section.id)"
                [class]="activeSection() === section.id 
                  ? 'text-indigo-600 dark:text-indigo-400 border-indigo-600 dark:border-indigo-400' 
                  : 'text-slate-500 dark:text-slate-400 border-transparent'"
                class="px-3 py-3 text-sm font-medium whitespace-nowrap border-b-2">
                {{ section.label }}
              </button>
            }
          </div>
        </div>

        <!-- Content -->
        <main class="flex-1 lg:border-l border-slate-200 dark:border-slate-800 min-h-[calc(100vh-3.5rem)]">
          <div class="max-w-xl p-4 lg:p-6">
            @switch (activeSection()) {
              @case ('account') {
                <section class="space-y-6">
                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Account</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 divide-y divide-slate-200 dark:divide-slate-800">
                      <button class="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 dark:hover:bg-slate-800/50">
                        <div class="flex items-center gap-3">
                          <app-icon name="mail" [size]="18" class="text-slate-400"></app-icon>
                          <span class="text-sm text-slate-900 dark:text-white">Email address</span>
                        </div>
                        <app-icon name="chevron-right" [size]="16" class="text-slate-400"></app-icon>
                      </button>
                      <button class="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 dark:hover:bg-slate-800/50">
                        <div class="flex items-center gap-3">
                          <app-icon name="lock" [size]="18" class="text-slate-400"></app-icon>
                          <span class="text-sm text-slate-900 dark:text-white">Change password</span>
                        </div>
                        <app-icon name="chevron-right" [size]="16" class="text-slate-400"></app-icon>
                      </button>
                    </div>
                  </div>

                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Danger Zone</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-red-200 dark:border-red-900/50">
                      <button class="w-full px-4 py-3 flex items-center justify-between hover:bg-red-50 dark:hover:bg-red-950/20">
                        <div class="flex items-center gap-3">
                          <app-icon name="trash-2" [size]="18" class="text-red-500"></app-icon>
                          <span class="text-sm text-red-600 dark:text-red-400">Delete account</span>
                        </div>
                        <app-icon name="chevron-right" [size]="16" class="text-red-400"></app-icon>
                      </button>
                    </div>
                  </div>
                </section>
              }

              @case ('privacy') {
                <section class="space-y-6">
                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Privacy</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 divide-y divide-slate-200 dark:divide-slate-800">
                      <div class="px-4 py-3 flex items-center justify-between">
                        <div>
                          <p class="text-sm text-slate-900 dark:text-white">Private account</p>
                          <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Only followers can see your posts</p>
                        </div>
                        <label class="relative inline-flex cursor-pointer">
                          <input type="checkbox" class="sr-only peer">
                          <div class="w-9 h-5 bg-slate-200 dark:bg-slate-700 rounded-full peer peer-checked:bg-indigo-600 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-transform peer-checked:after:translate-x-4"></div>
                        </label>
                      </div>
                      <button class="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 dark:hover:bg-slate-800/50">
                        <span class="text-sm text-slate-900 dark:text-white">Blocked accounts</span>
                        <app-icon name="chevron-right" [size]="16" class="text-slate-400"></app-icon>
                      </button>
                      <button class="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 dark:hover:bg-slate-800/50">
                        <span class="text-sm text-slate-900 dark:text-white">Muted accounts</span>
                        <app-icon name="chevron-right" [size]="16" class="text-slate-400"></app-icon>
                      </button>
                    </div>
                  </div>
                </section>
              }

              @case ('notifications') {
                <section class="space-y-6">
                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Push Notifications</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 divide-y divide-slate-200 dark:divide-slate-800">
                      @for (item of notificationItems; track item.key) {
                        <div class="px-4 py-3 flex items-center justify-between">
                          <span class="text-sm text-slate-900 dark:text-white">{{ item.label }}</span>
                          <label class="relative inline-flex cursor-pointer">
                            <input type="checkbox" [checked]="item.enabled" class="sr-only peer">
                            <div class="w-9 h-5 bg-slate-200 dark:bg-slate-700 rounded-full peer peer-checked:bg-indigo-600 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-transform peer-checked:after:translate-x-4"></div>
                          </label>
                        </div>
                      }
                    </div>
                  </div>
                </section>
              }

              @case ('media') {
                <section class="space-y-6">
                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Upload Providers</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 space-y-4">
                      <div>
                        <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Photos</label>
                        <select [(ngModel)]="photoProvider" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                          @for (p of photoProviders; track p.id) {
                            <option [value]="p.id">{{ p.name }}</option>
                          }
                        </select>
                      </div>
                      <div>
                        <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Videos</label>
                        <select [(ngModel)]="videoProvider" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                          @for (p of videoProviders; track p.id) {
                            <option [value]="p.id">{{ p.name }}</option>
                          }
                        </select>
                      </div>
                      <div>
                        <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Other files</label>
                        <select [(ngModel)]="otherProvider" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                          @for (p of otherProviders; track p.id) {
                            <option [value]="p.id">{{ p.name }}</option>
                          }
                        </select>
                      </div>
                    </div>
                  </div>

                  <!-- ImgBB Config -->
                  @if (photoProvider === 'imgbb') {
                    <div>
                      <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">ImgBB Configuration</h2>
                      <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4">
                        <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">API Key</label>
                        <div class="relative">
                          <input 
                            [(ngModel)]="imgbbConfig.apiKey"
                            [type]="showImgbbKey() ? 'text' : 'password'"
                            placeholder="Enter API key"
                            class="w-full px-3 py-2 pr-10 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                          <button (click)="showImgbbKey.set(!showImgbbKey())" class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600">
                            <app-icon [name]="showImgbbKey() ? 'eye-off' : 'eye'" [size]="16"></app-icon>
                          </button>
                        </div>
                        <p class="text-xs text-slate-500 mt-2">Get your key at <a href="https://api.imgbb.com/" target="_blank" class="text-indigo-600 dark:text-indigo-400 hover:underline">api.imgbb.com</a></p>
                      </div>
                    </div>
                  }

                  <!-- Cloudinary Config -->
                  @if (photoProvider === 'cloudinary' || videoProvider === 'cloudinary') {
                    <div>
                      <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Cloudinary Configuration</h2>
                      <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 space-y-3">
                        <p class="text-xs text-green-600 dark:text-green-400 flex items-center gap-1.5">
                          <app-icon name="check-circle" [size]="14"></app-icon>
                          Default configuration active
                        </p>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Cloud Name (optional)</label>
                          <input [(ngModel)]="cloudinaryConfig.cloudName" placeholder="your-cloud-name" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Upload Preset (optional)</label>
                          <input [(ngModel)]="cloudinaryConfig.uploadPreset" placeholder="your-preset" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                      </div>
                    </div>
                  }

                  <!-- R2 Config -->
                  @if (photoProvider === 'cloudflare-r2' || videoProvider === 'cloudflare-r2' || otherProvider === 'cloudflare-r2') {
                    <div>
                      <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Cloudflare R2 Configuration</h2>
                      <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 space-y-3">
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Account ID</label>
                          <input [(ngModel)]="cloudflareConfig.accountId" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Access Key ID</label>
                          <input [(ngModel)]="cloudflareConfig.accessKeyId" type="password" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Secret Access Key</label>
                          <input [(ngModel)]="cloudflareConfig.secretAccessKey" type="password" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Bucket Name</label>
                          <input [(ngModel)]="cloudflareConfig.bucketName" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                        <div>
                          <label class="block text-sm text-slate-700 dark:text-slate-300 mb-1.5">Public URL</label>
                          <input [(ngModel)]="cloudflareConfig.publicUrl" placeholder="https://pub-xxx.r2.dev" class="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500">
                        </div>
                      </div>
                    </div>
                  }

                  <button 
                    (click)="saveMediaConfig()"
                    [disabled]="isSaving()"
                    class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors">
                    {{ isSaving() ? 'Saving...' : 'Save changes' }}
                  </button>

                  <p class="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                    <app-icon name="shield" [size]="12"></app-icon>
                    Settings stored locally on your device
                  </p>
                </section>
              }

              @case ('display') {
                <section class="space-y-6">
                  <div>
                    <h2 class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-3">Appearance</h2>
                    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 divide-y divide-slate-200 dark:divide-slate-800">
                      <div class="px-4 py-3 flex items-center justify-between">
                        <div>
                          <p class="text-sm text-slate-900 dark:text-white">Launch fullscreen</p>
                          <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Open app in fullscreen mode</p>
                        </div>
                        <label class="relative inline-flex cursor-pointer">
                          <input type="checkbox" [checked]="fullscreenEnabled()" (change)="toggleFullscreen()" class="sr-only peer">
                          <div class="w-9 h-5 bg-slate-200 dark:bg-slate-700 rounded-full peer peer-checked:bg-indigo-600 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-transform peer-checked:after:translate-x-4"></div>
                        </label>
                      </div>
                    </div>
                  </div>
                </section>
              }
            }
          </div>
        </main>
      </div>

      <!-- Toast -->
      @if (toast()) {
        <div class="fixed bottom-20 left-1/2 -translate-x-1/2 z-50">
          <div [class]="toast()!.type === 'success' ? 'bg-slate-900 dark:bg-white text-white dark:text-slate-900' : 'bg-red-600 text-white'" class="px-4 py-2 rounded-lg text-sm font-medium shadow-lg">
            {{ toast()!.message }}
          </div>
        </div>
      }
    </div>
  `
})
export class SettingsComponent implements OnInit {
  private router = inject(Router);
  private imageUploadService = inject(ImageUploadService);

  activeSection = signal('account');
  isSaving = signal(false);
  showImgbbKey = signal(false);
  fullscreenEnabled = signal(false);
  toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  sections = [
    { id: 'account', label: 'Account', icon: 'user' },
    { id: 'privacy', label: 'Privacy', icon: 'shield' },
    { id: 'notifications', label: 'Notifications', icon: 'bell' },
    { id: 'media', label: 'Media', icon: 'image' },
    { id: 'display', label: 'Display', icon: 'monitor' }
  ];

  notificationItems = [
    { key: 'likes', label: 'Likes', enabled: true },
    { key: 'comments', label: 'Comments', enabled: true },
    { key: 'follows', label: 'New followers', enabled: true },
    { key: 'mentions', label: 'Mentions', enabled: true },
    { key: 'messages', label: 'Direct messages', enabled: true }
  ];

  photoProvider: ImageProvider = 'cloudinary';
  videoProvider: ImageProvider = 'cloudinary';
  otherProvider: ImageProvider = 'cloudflare-r2';

  imgbbConfig = { apiKey: '' };
  cloudinaryConfig = { cloudName: '', uploadPreset: '' };
  cloudflareConfig = { accountId: '', accessKeyId: '', secretAccessKey: '', bucketName: '', publicUrl: '' };

  allProviders = [
    { id: 'imgbb' as ImageProvider, name: 'ImgBB', supports: ['photo'] as FileType[] },
    { id: 'cloudinary' as ImageProvider, name: 'Cloudinary', supports: ['photo', 'video'] as FileType[] },
    { id: 'cloudflare-r2' as ImageProvider, name: 'Cloudflare R2', supports: ['photo', 'video', 'other'] as FileType[] }
  ];

  get photoProviders() { return this.allProviders.filter(p => p.supports.includes('photo')); }
  get videoProviders() { return this.allProviders.filter(p => p.supports.includes('video')); }
  get otherProviders() { return this.allProviders.filter(p => p.supports.includes('other')); }

  ngOnInit() {
    this.loadConfig();
    this.fullscreenEnabled.set(localStorage.getItem('fullscreenEnabled') === 'true');
  }

  loadConfig() {
    const providers = this.imageUploadService.getProviders();
    this.photoProvider = providers.photo;
    this.videoProvider = providers.video;
    this.otherProvider = providers.other;

    const config = this.imageUploadService.getConfig();
    if (config.imgbb) this.imgbbConfig.apiKey = config.imgbb.apiKey;
    if (config.cloudinary) {
      this.cloudinaryConfig.cloudName = config.cloudinary.cloudName;
      this.cloudinaryConfig.uploadPreset = config.cloudinary.uploadPreset;
    }
    if (config.cloudflareR2) {
      this.cloudflareConfig = { ...config.cloudflareR2 };
    }
  }

  saveMediaConfig() {
    this.isSaving.set(true);
    try {
      const config: ProviderConfig = {};
      if (this.imgbbConfig.apiKey.trim()) config.imgbb = { apiKey: this.imgbbConfig.apiKey.trim() };
      if (this.cloudinaryConfig.cloudName.trim() && this.cloudinaryConfig.uploadPreset.trim()) {
        config.cloudinary = { cloudName: this.cloudinaryConfig.cloudName.trim(), uploadPreset: this.cloudinaryConfig.uploadPreset.trim() };
      }
      if (this.cloudflareConfig.accountId.trim()) {
        config.cloudflareR2 = {
          accountId: this.cloudflareConfig.accountId.trim(),
          accessKeyId: this.cloudflareConfig.accessKeyId.trim(),
          secretAccessKey: this.cloudflareConfig.secretAccessKey.trim(),
          bucketName: this.cloudflareConfig.bucketName.trim(),
          publicUrl: this.cloudflareConfig.publicUrl.trim()
        };
      }
      this.imageUploadService.saveConfig(config);
      this.imageUploadService.setProviderForType('photo', this.photoProvider);
      this.imageUploadService.setProviderForType('video', this.videoProvider);
      this.imageUploadService.setProviderForType('other', this.otherProvider);
      this.showToast('Settings saved', 'success');
    } catch {
      this.showToast('Failed to save', 'error');
    } finally {
      this.isSaving.set(false);
    }
  }

  toggleFullscreen() {
    const enabled = !this.fullscreenEnabled();
    this.fullscreenEnabled.set(enabled);
    localStorage.setItem('fullscreenEnabled', enabled.toString());
    this.showToast(enabled ? 'Fullscreen enabled' : 'Fullscreen disabled', 'success');
  }

  goBack() {
    this.router.navigate(['/app/profile']);
  }

  private showToast(message: string, type: 'success' | 'error') {
    this.toast.set({ message, type });
    setTimeout(() => this.toast.set(null), 2000);
  }
}
