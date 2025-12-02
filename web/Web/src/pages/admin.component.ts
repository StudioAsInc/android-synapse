
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormArray } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DataService, Platform } from '../services/data.service';
import { IconComponent } from '../components/icon.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, IconComponent],
  template: `
    <div class="min-h-screen pt-20 pb-20 relative">
       <div class="absolute inset-0 -z-10">
          <div class="absolute inset-0 bg-grid opacity-10"></div>
       </div>

      <div class="container mx-auto px-4 sm:px-6 max-w-4xl">
        <div class="flex items-center justify-between mb-8 gap-4">
          <h1 class="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white">Admin Dashboard</h1>
          <button (click)="logout()" class="px-4 py-2 text-sm rounded-lg border transition-colors flex-shrink-0 touch-manipulation
                  bg-white text-slate-600 border-slate-200 hover:bg-slate-50
                  dark:bg-slate-800 dark:text-slate-300 dark:border-white/10 dark:hover:bg-slate-700 dark:hover:text-white">
            Sign Out
          </button>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <!-- Sidebar Info -->
          <div class="lg:col-span-1 space-y-6">
             <div class="p-4 sm:p-6 rounded-2xl border
                         bg-white border-slate-200
                         dark:bg-slate-900/50 dark:border-white/10">
               <div class="flex items-center gap-3 mb-4">
                 <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0
                             bg-indigo-50 text-indigo-600
                             dark:bg-indigo-500/20 dark:text-indigo-400">
                    <app-icon name="users" [size]="20"></app-icon>
                 </div>
                 <div class="min-w-0 flex-1">
                   <div class="text-sm text-slate-500 dark:text-slate-400">Logged in as</div>
                   <div class="font-medium truncate text-slate-900 dark:text-white">{{ authService.currentUser()?.email }}</div>
                 </div>
               </div>
               <div class="text-xs text-slate-500 dark:text-slate-400">
                 Use this panel to push updates to the changelog. Changes are live immediately.
               </div>
             </div>
          </div>

          <!-- Main Form -->
          <div class="lg:col-span-2">
            <div class="p-4 sm:p-6 lg:p-8 rounded-2xl border backdrop-blur-xl
                        bg-white/80 border-slate-200
                        dark:bg-slate-900/80 dark:border-white/10">
              <h2 class="text-lg sm:text-xl font-bold mb-6 flex items-center gap-2 text-slate-900 dark:text-white">
                <app-icon name="sparkles" class="text-yellow-500 dark:text-yellow-400" [size]="20"></app-icon>
                Add Changelog Entry
              </h2>

              <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-6">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label class="block text-sm font-medium mb-2 text-slate-700 dark:text-slate-400">Version</label>
                    <input type="text" formControlName="version" placeholder="e.g. 1.2.0" 
                           class="w-full px-4 py-3 border rounded-lg focus:border-indigo-500 focus:outline-none transition-colors
                                  bg-slate-50 border-slate-200 text-slate-900 placeholder-slate-400
                                  dark:bg-slate-950 dark:border-white/10 dark:text-white dark:placeholder-slate-600">
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-2 text-slate-700 dark:text-slate-400">Date</label>
                    <input type="text" formControlName="date" placeholder="e.g. Nov 12, 2025" 
                           class="w-full px-4 py-3 border rounded-lg focus:border-indigo-500 focus:outline-none transition-colors
                                  bg-slate-50 border-slate-200 text-slate-900 placeholder-slate-400
                                  dark:bg-slate-950 dark:border-white/10 dark:text-white dark:placeholder-slate-600">
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium mb-2 text-slate-700 dark:text-slate-400">Platform</label>
                  <div class="flex flex-wrap gap-2">
                    @for (p of platforms; track p) {
                       <button 
                         type="button"
                         (click)="setPlatform(p)"
                         class="px-3 py-1.5 rounded-lg text-sm border transition-all touch-manipulation active:scale-95"
                         [class]="form.get('platform')?.value === p 
                           ? 'bg-indigo-600 text-white border-indigo-500' 
                           : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-white hover:border-indigo-300 dark:bg-slate-950 dark:text-slate-400 dark:border-white/10 dark:hover:border-indigo-500/50'">
                         {{ p }}
                       </button>
                    }
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium mb-2 text-slate-700 dark:text-slate-400">Changes</label>
                  <div formArrayName="changes" class="space-y-3">
                    @for (change of changesControls; track $index) {
                      <div [formGroupName]="$index" class="flex gap-2">
                        <select formControlName="type" class="px-3 border rounded-lg text-sm focus:border-indigo-500 focus:outline-none
                                bg-slate-50 border-slate-200 text-slate-900
                                dark:bg-slate-950 dark:border-white/10 dark:text-white">
                          <option value="new">New</option>
                          <option value="fix">Fix</option>
                          <option value="improved">Improved</option>
                        </select>
                        <input type="text" formControlName="text" placeholder="Description of change..." 
                               class="flex-grow px-4 py-2 border rounded-lg focus:border-indigo-500 focus:outline-none
                                      bg-slate-50 border-slate-200 text-slate-900 placeholder-slate-400
                                      dark:bg-slate-950 dark:border-white/10 dark:text-white dark:placeholder-slate-600">
                        <button type="button" (click)="removeChange($index)" class="p-2 text-slate-400 hover:text-red-500 dark:text-slate-500 dark:hover:text-red-400 transition-colors touch-manipulation flex-shrink-0">
                          <app-icon name="x" [size]="18"></app-icon>
                        </button>
                      </div>
                    }
                  </div>
                  <button type="button" (click)="addChange()" class="mt-3 text-sm font-medium flex items-center gap-1 text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300 touch-manipulation">
                    <app-icon name="plus" [size]="14"></app-icon> Add another change
                  </button>
                </div>

                <div>
                  <label class="block text-sm font-medium mb-2 text-slate-700 dark:text-slate-400">Media (Images/Videos)</label>
                  <input #fileInput type="file" accept="image/*,video/*" multiple (change)="onFileSelect($event)" class="hidden">
                  <button type="button" (click)="fileInput.click()" class="w-full px-4 py-3 border-2 border-dashed rounded-lg transition-colors flex items-center justify-center gap-2 text-sm font-medium
                          border-slate-300 text-slate-600 hover:border-indigo-400 hover:text-indigo-600
                          dark:border-white/10 dark:text-slate-400 dark:hover:border-indigo-500 dark:hover:text-indigo-400">
                    <app-icon name="image-plus" [size]="20"></app-icon>
                    Upload Images or Videos
                  </button>
                  @if (mediaFiles().length > 0) {
                    <div class="mt-3 grid grid-cols-2 gap-2">
                      @for (file of mediaFiles(); track file.name; let i = $index) {
                        <div class="relative group rounded-lg overflow-hidden border border-slate-200 dark:border-white/10">
                          @if (file.type.startsWith('image/')) {
                            <img [src]="getFilePreview(file)" class="w-full h-24 object-cover">
                          } @else {
                            <div class="w-full h-24 bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                              <app-icon name="video" [size]="32" class="text-slate-400"></app-icon>
                            </div>
                          }
                          <button type="button" (click)="removeMedia(i)" class="absolute top-1 right-1 p-1 rounded bg-red-500 text-white opacity-0 group-hover:opacity-100 transition-opacity">
                            <app-icon name="x" [size]="14"></app-icon>
                          </button>
                          <div class="px-2 py-1 text-xs truncate bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-400">
                            {{ file.name }}
                          </div>
                        </div>
                      }
                    </div>
                  }
                </div>

                <div class="pt-4 border-t border-slate-200 dark:border-white/5 flex justify-end">
                   <button 
                     type="submit" 
                     [disabled]="form.invalid || submitting()"
                     class="px-6 py-3 font-bold rounded-lg disabled:opacity-50 transition-all touch-manipulation
                            bg-slate-900 text-white hover:bg-slate-800 active:scale-95
                            dark:bg-white dark:text-slate-950 dark:hover:bg-indigo-50">
                     {{ submitting() ? 'Publishing...' : 'Publish Update' }}
                   </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AdminComponent {
  authService = inject(AuthService);
  private dataService = inject(DataService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  submitting = signal(false);
  platforms: Platform[] = ['Android', 'iOS', 'Web', 'Windows', 'Linux'];

  form = this.fb.group({
    version: ['', Validators.required],
    date: [new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }), Validators.required],
    platform: ['Web', Validators.required],
    changes: this.fb.array([]),
    media: this.fb.array([])
  });

  mediaFiles = signal<File[]>([]);

  constructor() {
    this.addChange();
  }

  get changesArray() {
    return this.form.get('changes') as FormArray;
  }

  get mediaArray() {
    return this.form.get('media') as FormArray;
  }

  get changesControls() {
    return this.changesArray.controls;
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.mediaFiles.set(Array.from(input.files));
    }
  }

  removeMedia(index: number) {
    const files = this.mediaFiles();
    files.splice(index, 1);
    this.mediaFiles.set([...files]);
  }

  getFilePreview(file: File): string {
    return URL.createObjectURL(file);
  }

  private async uploadToCloudinary(file: File): Promise<string> {
    // Use ImgBB for images
    if (file.type.startsWith('image/')) {
      const formData = new FormData();
      formData.append('image', file);
      
      const response = await fetch('https://api.imgbb.com/1/upload?key=46c28b365f88957c90d0c41ba2d47f13', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) throw new Error('Image upload failed');
      const data = await response.json();
      return data.data.url;
    }
    
    // Use Cloudinary for videos
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', 'synapse');
    
    const response = await fetch('https://api.cloudinary.com/v1_1/djw3fgbls/video/upload', {
      method: 'POST',
      body: formData
    });
    
    if (!response.ok) throw new Error('Video upload failed');
    const data = await response.json();
    return data.secure_url;
  }

  addChange() {
    const changeGroup = this.fb.group({
      type: ['new', Validators.required],
      text: ['', Validators.required]
    });
    this.changesArray.push(changeGroup);
  }

  removeChange(index: number) {
    this.changesArray.removeAt(index);
  }

  setPlatform(p: Platform) {
    this.form.patchValue({ platform: p });
  }

  async submit() {
    if (this.form.invalid) return;
    
    this.submitting.set(true);
    
    // Create a promise that rejects after 5 seconds to prevent indefinite hanging
    const timeout = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Request timed out. Check internet connection.')), 5000)
    );

    try {
      const val = this.form.value;
      
      // Upload media files if any
      const mediaUrls: { type: 'image' | 'video'; url: string }[] = [];
      for (const file of this.mediaFiles()) {
        const url = await this.uploadToCloudinary(file);
        mediaUrls.push({
          type: file.type.startsWith('image/') ? 'image' : 'video',
          url
        });
      }
      
      const payload = {
        version: val.version!,
        date: val.date!,
        platform: val.platform as Platform,
        changes: val.changes as any[],
        media: mediaUrls.length > 0 ? mediaUrls : undefined,
        createdAt: Date.now()
      };

      // Race between the actual request and the timeout
      await Promise.race([
        this.dataService.addChangelog(payload),
        timeout
      ]);
      
      alert('Update published successfully!');
      this.form.reset({
        platform: 'Web',
        date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
      });
      this.changesArray.clear();
      this.mediaFiles.set([]);
      this.addChange();

    } catch (err: any) {
      console.error('Publish Error:', err);
      alert(`Error publishing update: ${err.message || err}`);
    } finally {
      this.submitting.set(false);
    }
  }

  async logout() {
    await this.authService.logout();
    this.router.navigate(['/']);
  }
}
