import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IconComponent } from '../components/icon.component';
import { AuthService } from '../services/auth.service';
import { SupabaseService } from '../services/supabase.service';

type StoryType = 'IMAGE' | 'VIDEO' | 'TEXT';
type FilterType = 'none' | 'grayscale' | 'sepia' | 'blur' | 'brightness' | 'contrast' | 'saturate';

interface PollOption {
  id: string;
  text: string;
}

@Component({
  selector: 'app-story-compose',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="fixed inset-0 bg-black z-50 flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 bg-gradient-to-b from-black/80 to-transparent absolute top-0 left-0 right-0 z-10">
        <button (click)="goBack()" class="p-2 hover:bg-white/10 rounded-full transition-colors">
          <app-icon name="x" [size]="24" class="text-white"></app-icon>
        </button>
        <div class="flex items-center gap-2">
          <button (click)="togglePrivacy()" class="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-full text-white text-sm font-medium backdrop-blur-sm">
            <app-icon [name]="isPrivate() ? 'lock' : 'globe'" [size]="16" class="inline mr-1"></app-icon>
            {{ isPrivate() ? 'Close Friends' : 'Everyone' }}
          </button>
          <button 
            (click)="publishStory()"
            [disabled]="!canPublish() || isPublishing()"
            class="px-6 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed rounded-full text-white font-bold">
            {{ isPublishing() ? 'Publishing...' : 'Share' }}
          </button>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="flex-1 flex items-center justify-center relative">
        @if (mediaUrl()) {
          <div class="relative max-w-lg w-full h-full flex items-center justify-center">
            @if (storyType() === 'IMAGE') {
              <img 
                [src]="mediaUrl()" 
                [style.filter]="getFilterStyle()"
                class="max-h-full max-w-full object-contain">
            } @else if (storyType() === 'VIDEO') {
              <video 
                [src]="mediaUrl()" 
                [style.filter]="getFilterStyle()"
                class="max-h-full max-w-full object-contain"
                controls
                autoplay
                muted
                loop>
              </video>
            }

            <!-- Text Overlay -->
            @if (storyText()) {
              <div 
                class="absolute inset-0 flex items-center justify-center p-8"
                [style.background]="textBackground()">
                <p 
                  class="text-center font-bold break-words max-w-md"
                  [style.color]="textColor()"
                  [style.font-size.px]="textSize()">
                  {{ storyText() }}
                </p>
              </div>
            }

            <!-- Poll Overlay -->
            @if (showPoll() && pollOptions().length > 0) {
              <div class="absolute bottom-20 left-4 right-4 space-y-2">
                <p class="text-white font-bold text-lg mb-3">{{ pollQuestion() }}</p>
                @for (option of pollOptions(); track option.id) {
                  <div class="bg-white/20 backdrop-blur-md rounded-full px-4 py-3 text-white font-medium">
                    {{ option.text }}
                  </div>
                }
              </div>
            }

            <!-- Question Sticker -->
            @if (showQuestion()) {
              <div class="absolute bottom-20 left-4 right-4 bg-white/90 backdrop-blur-md rounded-2xl p-4">
                <p class="text-slate-800 font-bold mb-2">{{ questionText() }}</p>
                <input 
                  type="text" 
                  placeholder="Type your answer..."
                  class="w-full px-3 py-2 bg-slate-100 rounded-full text-sm"
                  disabled>
              </div>
            }

            <!-- Location Tag -->
            @if (locationName()) {
              <div class="absolute top-20 left-4 bg-black/50 backdrop-blur-md rounded-full px-4 py-2 flex items-center gap-2">
                <app-icon name="map-pin" [size]="16" class="text-white"></app-icon>
                <span class="text-white text-sm font-medium">{{ locationName() }}</span>
              </div>
            }

            <!-- Mentions -->
            @if (mentions().length > 0) {
              <div class="absolute bottom-4 left-4 flex flex-wrap gap-2">
                @for (mention of mentions(); track mention) {
                  <div class="bg-black/50 backdrop-blur-md rounded-full px-3 py-1 text-white text-sm">
                    @{{ mention }}
                  </div>
                }
              </div>
            }
          </div>
        } @else if (storyType() === 'TEXT') {
          <div 
            class="w-full h-full flex items-center justify-center p-8"
            [style.background]="textBackground()">
            <textarea
              [(ngModel)]="storyText"
              placeholder="Type something..."
              class="w-full max-w-md text-center font-bold bg-transparent border-none outline-none resize-none"
              [style.color]="textColor()"
              [style.font-size.px]="textSize()"
              rows="6"
              maxlength="200">
            </textarea>
          </div>
        } @else {
          <div class="text-center text-white/60">
            <app-icon name="image" [size]="64" class="mx-auto mb-4 opacity-50"></app-icon>
            <p class="text-lg">Choose media or create text story</p>
          </div>
        }
      </div>

      <!-- Bottom Toolbar -->
      <div class="bg-gradient-to-t from-black/80 to-transparent p-4 absolute bottom-0 left-0 right-0">
        <div class="flex items-center justify-around max-w-2xl mx-auto">
          <label class="flex flex-col items-center gap-1 cursor-pointer hover:opacity-80">
            <input type="file" accept="image/*" (change)="onMediaSelect($event, 'IMAGE')" class="hidden">
            <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <app-icon name="image" [size]="24" class="text-white"></app-icon>
            </div>
            <span class="text-white text-xs">Photo</span>
          </label>

          <label class="flex flex-col items-center gap-1 cursor-pointer hover:opacity-80">
            <input type="file" accept="video/*" (change)="onMediaSelect($event, 'VIDEO')" class="hidden">
            <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <app-icon name="video" [size]="24" class="text-white"></app-icon>
            </div>
            <span class="text-white text-xs">Video</span>
          </label>

          <button (click)="createTextStory()" class="flex flex-col items-center gap-1 hover:opacity-80">
            <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <app-icon name="type" [size]="24" class="text-white"></app-icon>
            </div>
            <span class="text-white text-xs">Text</span>
          </button>

          <button (click)="toggleFilterMenu()" class="flex flex-col items-center gap-1 hover:opacity-80">
            <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <app-icon name="sliders" [size]="24" class="text-white"></app-icon>
            </div>
            <span class="text-white text-xs">Filter</span>
          </button>

          <button (click)="toggleToolsMenu()" class="flex flex-col items-center gap-1 hover:opacity-80">
            <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <app-icon name="smile" [size]="24" class="text-white"></app-icon>
            </div>
            <span class="text-white text-xs">Tools</span>
          </button>
        </div>
      </div>

      <!-- Filter Menu -->
      @if (showFilterMenu()) {
        <div class="absolute bottom-24 left-0 right-0 bg-black/90 backdrop-blur-xl p-4 animate-in slide-in-from-bottom">
          <div class="flex items-center gap-3 overflow-x-auto pb-2">
            @for (filter of filters; track filter.value) {
              <button 
                (click)="applyFilter(filter.value)"
                class="flex-shrink-0 flex flex-col items-center gap-2"
                [class.opacity-100]="currentFilter() === filter.value"
                [class.opacity-60]="currentFilter() !== filter.value">
                <div class="w-16 h-16 rounded-lg bg-white/10 flex items-center justify-center text-white text-xs font-medium">
                  {{ filter.label }}
                </div>
              </button>
            }
          </div>
        </div>
      }

      <!-- Tools Menu -->
      @if (showToolsMenu()) {
        <div class="absolute bottom-24 left-0 right-0 bg-black/90 backdrop-blur-xl p-4 animate-in slide-in-from-bottom">
          <div class="grid grid-cols-4 gap-4 max-w-md mx-auto">
            <button (click)="openPollCreator()" class="flex flex-col items-center gap-2 hover:opacity-80">
              <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
                <app-icon name="bar-chart" [size]="24" class="text-white"></app-icon>
              </div>
              <span class="text-white text-xs">Poll</span>
            </button>

            <button (click)="openQuestionSticker()" class="flex flex-col items-center gap-2 hover:opacity-80">
              <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
                <app-icon name="help-circle" [size]="24" class="text-white"></app-icon>
              </div>
              <span class="text-white text-xs">Question</span>
            </button>

            <button (click)="openLocationPicker()" class="flex flex-col items-center gap-2 hover:opacity-80">
              <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
                <app-icon name="map-pin" [size]="24" class="text-white"></app-icon>
              </div>
              <span class="text-white text-xs">Location</span>
            </button>

            <button (click)="openMentionPicker()" class="flex flex-col items-center gap-2 hover:opacity-80">
              <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
                <app-icon name="at-sign" [size]="24" class="text-white"></app-icon>
              </div>
              <span class="text-white text-xs">Mention</span>
            </button>
          </div>
        </div>
      }

      <!-- Poll Creator Modal -->
      @if (showPollCreator()) {
        <div class="absolute inset-0 bg-black/80 flex items-end justify-center z-20" (click)="closePollCreator()">
          <div class="bg-slate-900 w-full max-w-lg rounded-t-3xl p-6 animate-in slide-in-from-bottom" (click)="$event.stopPropagation()">
            <h3 class="text-white font-bold text-xl mb-4">Create Poll</h3>
            <input 
              [(ngModel)]="pollQuestion"
              type="text" 
              placeholder="Ask a question..."
              class="w-full px-4 py-3 bg-slate-800 text-white rounded-xl mb-4">
            
            @for (option of pollOptions(); track option.id; let i = $index) {
              <div class="flex gap-2 mb-2">
                <input 
                  [(ngModel)]="option.text"
                  type="text" 
                  [placeholder]="'Option ' + (i + 1)"
                  class="flex-1 px-4 py-3 bg-slate-800 text-white rounded-xl">
                @if (pollOptions().length > 2) {
                  <button (click)="removePollOption(i)" class="p-3 hover:bg-red-500/20 rounded-xl">
                    <app-icon name="x" [size]="20" class="text-red-500"></app-icon>
                  </button>
                }
              </div>
            }

            @if (pollOptions().length < 4) {
              <button (click)="addPollOption()" class="w-full py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl mb-4">
                + Add Option
              </button>
            }

            <div class="flex gap-2">
              <button (click)="savePoll()" class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-xl">
                Add Poll
              </button>
              <button (click)="closePollCreator()" class="px-6 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl">
                Cancel
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Question Sticker Modal -->
      @if (showQuestionModal()) {
        <div class="absolute inset-0 bg-black/80 flex items-end justify-center z-20" (click)="closeQuestionModal()">
          <div class="bg-slate-900 w-full max-w-lg rounded-t-3xl p-6 animate-in slide-in-from-bottom" (click)="$event.stopPropagation()">
            <h3 class="text-white font-bold text-xl mb-4">Add Question</h3>
            <input 
              [(ngModel)]="questionText"
              type="text" 
              placeholder="Ask me anything..."
              class="w-full px-4 py-3 bg-slate-800 text-white rounded-xl mb-4">
            
            <div class="flex gap-2">
              <button (click)="saveQuestion()" class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-xl">
                Add Question
              </button>
              <button (click)="closeQuestionModal()" class="px-6 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl">
                Cancel
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class StoryComposeComponent {
  private router = inject(Router);
  private auth = inject(AuthService);
  private supabase = inject(SupabaseService).client;

  storyType = signal<StoryType | null>(null);
  mediaUrl = signal<string>('');
  mediaFile = signal<File | null>(null);
  storyText = signal('');
  
  currentFilter = signal<FilterType>('none');
  textColor = signal('#ffffff');
  textSize = signal(32);
  textBackground = signal('linear-gradient(135deg, #667eea 0%, #764ba2 100%)');
  
  isPrivate = signal(false);
  isPublishing = signal(false);
  
  showFilterMenu = signal(false);
  showToolsMenu = signal(false);
  showPollCreator = signal(false);
  showQuestionModal = signal(false);
  
  showPoll = signal(false);
  pollQuestion = signal('');
  pollOptions = signal<PollOption[]>([
    { id: '1', text: '' },
    { id: '2', text: '' }
  ]);
  
  showQuestion = signal(false);
  questionText = signal('');
  
  locationName = signal('');
  mentions = signal<string[]>([]);

  filters = [
    { label: 'Normal', value: 'none' as FilterType },
    { label: 'B&W', value: 'grayscale' as FilterType },
    { label: 'Sepia', value: 'sepia' as FilterType },
    { label: 'Blur', value: 'blur' as FilterType },
    { label: 'Bright', value: 'brightness' as FilterType },
    { label: 'Contrast', value: 'contrast' as FilterType },
    { label: 'Vibrant', value: 'saturate' as FilterType }
  ];

  goBack() {
    this.router.navigate(['/app/feed']);
  }

  togglePrivacy() {
    this.isPrivate.update(v => !v);
  }

  onMediaSelect(event: Event, type: StoryType) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.storyType.set(type);
    this.mediaFile.set(file);
    this.mediaUrl.set(URL.createObjectURL(file));
  }

  createTextStory() {
    this.storyType.set('TEXT');
    this.mediaUrl.set('');
  }

  toggleFilterMenu() {
    this.showFilterMenu.update(v => !v);
    this.showToolsMenu.set(false);
  }

  toggleToolsMenu() {
    this.showToolsMenu.update(v => !v);
    this.showFilterMenu.set(false);
  }

  applyFilter(filter: FilterType) {
    this.currentFilter.set(filter);
  }

  getFilterStyle(): string {
    const filters: Record<FilterType, string> = {
      'none': 'none',
      'grayscale': 'grayscale(100%)',
      'sepia': 'sepia(100%)',
      'blur': 'blur(2px)',
      'brightness': 'brightness(1.2)',
      'contrast': 'contrast(1.3)',
      'saturate': 'saturate(1.5)'
    };
    return filters[this.currentFilter()];
  }

  openPollCreator() {
    this.showPollCreator.set(true);
    this.showToolsMenu.set(false);
  }

  closePollCreator() {
    this.showPollCreator.set(false);
  }

  addPollOption() {
    if (this.pollOptions().length >= 4) return;
    this.pollOptions.update(opts => [...opts, { id: Date.now().toString(), text: '' }]);
  }

  removePollOption(index: number) {
    this.pollOptions.update(opts => opts.filter((_, i) => i !== index));
  }

  savePoll() {
    if (!this.pollQuestion().trim() || this.pollOptions().some(o => !o.text.trim())) {
      alert('Please fill in all poll fields');
      return;
    }
    this.showPoll.set(true);
    this.closePollCreator();
  }

  openQuestionSticker() {
    this.showQuestionModal.set(true);
    this.showToolsMenu.set(false);
  }

  closeQuestionModal() {
    this.showQuestionModal.set(false);
  }

  saveQuestion() {
    if (!this.questionText().trim()) {
      alert('Please enter a question');
      return;
    }
    this.showQuestion.set(true);
    this.closeQuestionModal();
  }

  openLocationPicker() {
    const location = prompt('Enter location:');
    if (location) {
      this.locationName.set(location);
    }
    this.showToolsMenu.set(false);
  }

  openMentionPicker() {
    const username = prompt('Enter username to mention:');
    if (username) {
      this.mentions.update(m => [...m, username.replace('@', '')]);
    }
    this.showToolsMenu.set(false);
  }

  canPublish(): boolean {
    return !!(this.mediaUrl() || this.storyText().trim());
  }

  async publishStory() {
    if (!this.canPublish() || this.isPublishing()) return;

    this.isPublishing.set(true);
    try {
      const userId = this.auth.currentUser()?.id;
      if (!userId) throw new Error('Not authenticated');

      let mediaUrl = this.mediaUrl();

      // Upload media if exists
      if (this.mediaFile()) {
        const file = this.mediaFile()!;
        const fileName = `${userId}/${Date.now()}_${file.name}`;
        
        const { data, error } = await this.supabase.storage
          .from('story-media')
          .upload(fileName, file);

        if (error) throw error;

        const { data: { publicUrl } } = this.supabase.storage
          .from('story-media')
          .getPublicUrl(fileName);

        mediaUrl = publicUrl;
      }

      // Create story
      const { error } = await this.supabase
        .from('stories')
        .insert({
          user_id: userId,
          media_url: mediaUrl,
          media_type: this.storyType(),
          content: this.storyText() || null,
          privacy_setting: this.isPrivate() ? 'close_friends' : 'followers'
        });

      if (error) throw error;

      alert('Story published!');
      this.router.navigate(['/app/feed']);
    } catch (err) {
      console.error('Error publishing story:', err);
      alert('Failed to publish story');
    } finally {
      this.isPublishing.set(false);
    }
  }
}
