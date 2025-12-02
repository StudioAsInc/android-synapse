import { Component, input, signal, inject, OnInit, OnDestroy, ElementRef, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from './icon.component';
import { CommentItemComponent } from './comment-item.component';
import { CommentShimmerComponent } from './ui/shimmer.component';
import { CommentService } from '../services/comment.service';
import { AuthService } from '../services/auth.service';
import { SocialService } from '../services/social.service';
import { RealtimeService } from '../services/realtime.service';
import { ImageUploadService } from '../services/image-upload.service';

@Component({
  selector: 'app-comment-section',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, CommentItemComponent, CommentShimmerComponent],
  template: `
    <div class="border-t border-slate-200 dark:border-white/10 pb-20 sm:pb-0">
      <!-- Sort Options -->
      <div class="px-3 sm:px-4 py-2.5 sm:py-3 border-b border-slate-200 dark:border-white/10 flex items-center justify-between gap-2">
        <h3 class="font-bold text-sm sm:text-base text-slate-900 dark:text-white whitespace-nowrap">
          Comments ({{ comments().length }})
        </h3>
        
        <div class="flex gap-1 overflow-x-auto no-scrollbar">
          @for (opt of sortOptions; track opt.value) {
            <button 
              (click)="sortBy.set(opt.value); updateSortedComments()"
              [class.bg-indigo-100]="sortBy() === opt.value"
              [class.dark:bg-indigo-900/30]="sortBy() === opt.value"
              [class.text-indigo-600]="sortBy() === opt.value"
              [class.dark:text-indigo-400]="sortBy() === opt.value"
              class="px-3 py-1.5 min-h-[36px] text-xs font-medium rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400 transition-colors whitespace-nowrap active:scale-95">
              {{ opt.label }}
            </button>
          }
        </div>
      </div>

      <!-- Comments List -->
      <div class="divide-y divide-slate-200 dark:divide-white/10">
        @if (commentService.loading()) {
          <div class="p-3 sm:p-4">
            @for (i of [1,2,3]; track i) {
              <app-comment-shimmer></app-comment-shimmer>
            }
          </div>
        } @else if (sortedComments().length === 0) {
          <div class="p-8 text-center text-slate-500">
            <app-icon name="message-circle" [size]="48" class="mx-auto mb-2 opacity-30"></app-icon>
            <p class="font-medium">No comments yet</p>
            <p class="text-sm mt-1">Be the first to share your thoughts!</p>
          </div>
        } @else {
          <div class="px-3 sm:px-4">
            @for (comment of sortedComments(); track comment.id) {
              <app-comment-item 
                [comment]="comment" 
                [postId]="postId()"
                (commentUpdated)="loadComments()"
              ></app-comment-item>
            }
          </div>
        }
      </div>

      <!-- Mobile Fixed Bottom Input -->
      <div class="fixed bottom-0 left-0 right-0 sm:relative sm:bottom-auto border-t border-slate-200 dark:border-white/10 bg-white dark:bg-slate-950 p-2 sm:p-4 z-30 safe-area-bottom">
        <!-- Image Preview -->
        @if (selectedImage()) {
          <div class="mb-2 relative inline-block ml-12">
            <img [src]="selectedImage()" class="max-h-20 sm:max-h-32 rounded-lg">
            <button 
              (click)="removeImage()"
              class="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1.5 min-w-[28px] min-h-[28px] flex items-center justify-center active:scale-95">
              <app-icon name="x" [size]="14"></app-icon>
            </button>
          </div>
        }

        <div class="flex gap-2 items-end">
          <img [src]="socialService.currentUser().avatar" 
               class="w-9 h-9 sm:w-10 sm:h-10 rounded-full object-cover flex-shrink-0">
          
          <div class="flex-1 flex items-end gap-2 bg-slate-100 dark:bg-slate-900 rounded-2xl px-3 py-2">
            <textarea 
              [(ngModel)]="commentText"
              class="flex-1 bg-transparent text-sm text-slate-900 dark:text-white resize-none focus:outline-none placeholder:text-slate-500 max-h-24"
              rows="1"
              placeholder="Add a comment..."
              (input)="autoResize($event)"
              (keydown.enter)="!$event.shiftKey && submitComment(); $event.shiftKey || $event.preventDefault()"
            ></textarea>
            
            <div class="flex items-center gap-1 flex-shrink-0">
              <input #fileInput type="file" accept="image/*" (change)="onFileSelected($event)" class="hidden">
              <button 
                (click)="fileInput.click()"
                [disabled]="isUploading()"
                class="p-2 min-w-[40px] min-h-[40px] rounded-full text-slate-500 hover:text-indigo-500 active:bg-slate-200 dark:active:bg-slate-800 transition-colors disabled:opacity-50 flex items-center justify-center">
                <app-icon name="image" [size]="20"></app-icon>
              </button>
            </div>
          </div>
          
          <button 
            (click)="submitComment()"
            [disabled]="(!commentText().trim() && !selectedImage()) || isSubmitting() || isUploading()"
            class="p-2.5 min-w-[44px] min-h-[44px] bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white rounded-full transition-colors active:scale-95 flex items-center justify-center flex-shrink-0">
            @if (isUploading() || isSubmitting()) {
              <app-icon name="loader" [size]="20" class="animate-spin"></app-icon>
            } @else {
              <app-icon name="send" [size]="20"></app-icon>
            }
          </button>
        </div>
      </div>
    </div>
  `
})
export class CommentSectionComponent implements OnInit, OnDestroy {
  postId = input.required<string>();

  commentService = inject(CommentService);
  authService = inject(AuthService);
  socialService = inject(SocialService);
  private realtimeService = inject(RealtimeService);
  private uploadService = inject(ImageUploadService);

  commentText = signal('');
  isSubmitting = signal(false);
  isUploading = signal(false);
  sortBy = signal<'featured' | 'newest' | 'oldest'>('featured');
  selectedImage = signal<string | null>(null);
  selectedFile = signal<File | null>(null);
  
  comments = this.commentService.comments;
  sortedComments = signal<any[]>([]);

  sortOptions = [
    { value: 'featured' as const, label: 'Featured' },
    { value: 'newest' as const, label: 'Newest' },
    { value: 'oldest' as const, label: 'Oldest' }
  ];

  autoResize(event: Event) {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 96) + 'px';
  }

  ngOnInit() {
    this.loadComments();
    
    // Subscribe to real-time comment updates
    this.realtimeService.subscribeToComments(this.postId(), (payload) => {
      console.log('Comment update:', payload);
      this.loadComments();
    });
  }

  ngOnDestroy() {
    this.realtimeService.unsubscribe(`comments:${this.postId()}`);
  }

  async loadComments() {
    await this.commentService.fetchComments(this.postId());
    this.updateSortedComments();
  }

  updateSortedComments() {
    const comments = [...this.comments()];
    const sort = this.sortBy();
    
    switch (sort) {
      case 'newest':
        comments.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
        break;
      case 'oldest':
        comments.sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime());
        break;
      case 'featured':
      default:
        // Sort by likes, then by date
        comments.sort((a, b) => {
          if (b.likes_count !== a.likes_count) {
            return b.likes_count - a.likes_count;
          }
          return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
        });
        break;
    }
    
    this.sortedComments.set(comments);
  }

  async submitComment() {
    if ((!this.commentText().trim() && !this.selectedImage()) || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    try {
      let mediaUrl: string | undefined;
      
      // Upload image if selected
      if (this.selectedFile()) {
        this.isUploading.set(true);
        try {
          mediaUrl = await this.uploadService.uploadImage(this.selectedFile()!);
        } catch (err) {
          console.error('Error uploading image:', err);
          alert('Failed to upload image. Please try again.');
          return;
        } finally {
          this.isUploading.set(false);
        }
      }

      await this.commentService.createComment(this.postId(), this.commentText(), undefined, mediaUrl);
      this.commentText.set('');
      this.removeImage();
      await this.loadComments();
    } catch (err) {
      console.error('Error posting comment:', err);
      alert('Failed to post comment. Please try again.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    
    if (file) {
      if (!file.type.startsWith('image/')) {
        alert('Please select an image file');
        return;
      }
      
      if (file.size > 10 * 1024 * 1024) {
        alert('Image size must be less than 10MB');
        return;
      }

      this.selectedFile.set(file);
      
      const reader = new FileReader();
      reader.onload = (e) => {
        this.selectedImage.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage() {
    this.selectedImage.set(null);
    this.selectedFile.set(null);
  }
}
