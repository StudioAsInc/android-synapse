import { Component, input, signal, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from './icon.component';
import { ReactionPickerComponent, ReactionType } from './reaction-picker.component';
import { ActionMenuComponent, MenuItem } from './action-menu.component';
import { TextFormatterComponent } from './text-formatter.component';
import { Comment, CommentService } from '../services/comment.service';
import { AuthService } from '../services/auth.service';
import { TextParserService } from '../services/text-parser.service';

@Component({
  selector: 'app-comment-item',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, ReactionPickerComponent, ActionMenuComponent, TextFormatterComponent],
  template: `
    <div class="flex gap-2.5 sm:gap-3 py-3 sm:py-3" [class.ml-6]="isReply()" [class.sm:ml-10]="isReply()">
      <!-- Avatar -->
      <img [src]="comment().user?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'" 
           class="w-8 h-8 sm:w-10 sm:h-10 rounded-full object-cover flex-shrink-0">
      
      <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex items-center gap-1.5 mb-1 flex-wrap">
          <span class="font-bold text-[13px] sm:text-sm text-slate-900 dark:text-white">
            {{ comment().user?.display_name || 'Unknown' }}
          </span>
          @if (comment().user?.verify) {
            <app-icon name="verified" [size]="14" class="text-indigo-500"></app-icon>
          }
          <span class="text-slate-500 text-[11px] sm:text-xs">
            @{{ comment().user?.username || 'unknown' }} · {{ formatDate(comment().created_at) }}
            @if (comment().is_edited) { <span class="italic">(edited)</span> }
          </span>
        </div>

        <!-- Content -->
        @if (!isEditing()) {
          <p class="text-slate-800 dark:text-slate-200 text-[13px] sm:text-sm mb-2 whitespace-pre-wrap leading-relaxed">
            <app-text-formatter 
              [text]="comment().content"
              [segments]="parseText(comment().content)"
              (mentionClicked)="handleMentionClick($event)"
              (hashtagClicked)="handleHashtagClick($event)">
            </app-text-formatter>
          </p>
        } @else {
          <div class="mb-2">
            <textarea 
              [(ngModel)]="editText"
              class="w-full p-2.5 text-sm border border-slate-300 dark:border-white/20 rounded-xl bg-white dark:bg-slate-900 text-slate-900 dark:text-white resize-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              rows="3"
            ></textarea>
            <div class="flex gap-2 mt-2">
              <button 
                (click)="saveEdit()"
                [disabled]="!editText.trim()"
                class="px-4 py-2 min-h-[40px] bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-medium rounded-full active:scale-95">
                Save
              </button>
              <button 
                (click)="cancelEdit()"
                class="px-4 py-2 min-h-[40px] bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs font-medium rounded-full active:scale-95">
                Cancel
              </button>
            </div>
          </div>
        }

        <!-- Media -->
        @if (comment().media_url) {
          <div class="mb-2 rounded-xl overflow-hidden border border-slate-200 dark:border-white/10 max-w-xs">
            @if (comment().media_type === 'IMAGE' || comment().media_type === 'GIF') {
              <img [src]="comment().media_url" class="w-full h-auto">
            } @else if (comment().media_type === 'VIDEO') {
              <video [src]="comment().media_url" controls class="w-full h-auto"></video>
            } @else if (comment().media_type === 'VOICE') {
              <audio [src]="comment().media_url" controls class="w-full"></audio>
            }
          </div>
        }

        <!-- Reactions Display -->
        @if (comment().likes_count > 0) {
          <div class="flex items-center gap-1 mb-2">
            @if (currentReaction()) {
              <span class="text-sm">{{ getReactionEmoji(currentReaction()) }}</span>
            }
            <span class="text-xs text-slate-500">{{ comment().likes_count }}</span>
          </div>
        }

        <!-- Actions - Mobile Optimized -->
        <div class="flex items-center gap-1 text-slate-500">
          <app-reaction-picker 
            (reactionSelected)="handleReaction($event)"
            [triggerClass]="'px-3 py-2 min-h-[40px] rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 active:scale-95 transition-all text-xs font-medium flex items-center ' + getReactionColor(currentReaction())">
            @if (currentReaction()) {
              <span class="flex items-center gap-1">
                <span>{{ getReactionEmoji(currentReaction()) }}</span>
                <span class="hidden sm:inline">{{ currentReaction() === 'LIKE' ? 'Like' : currentReaction() }}</span>
              </span>
            } @else {
              <span>Like</span>
            }
          </app-reaction-picker>
          
          <button 
            (click)="toggleReply()"
            class="px-3 py-2 min-h-[40px] rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-indigo-500 active:scale-95 transition-all text-xs font-medium">
            Reply
          </button>

          <app-action-menu 
            [items]="menuItems()"
            (itemSelected)="handleMenuAction($event)">
          </app-action-menu>
        </div>

        <!-- Reply Input -->
        @if (showReplyInput()) {
          <div class="mt-3 flex gap-2">
            <img [src]="authService.currentUser()?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'" 
                 class="w-7 h-7 sm:w-8 sm:h-8 rounded-full object-cover flex-shrink-0">
            <div class="flex-1">
              <div class="flex items-end gap-2 bg-slate-100 dark:bg-slate-900 rounded-2xl px-3 py-2">
                <textarea 
                  [(ngModel)]="replyText"
                  class="flex-1 bg-transparent text-sm text-slate-900 dark:text-white resize-none focus:outline-none placeholder:text-slate-500 max-h-20"
                  rows="1"
                  placeholder="Write a reply..."
                  (input)="autoResizeReply($event)"
                  (keydown.enter)="!$event.shiftKey && submitReply(); $event.shiftKey || $event.preventDefault()"
                ></textarea>
                <button 
                  (click)="submitReply()"
                  [disabled]="!replyText.trim() || isSubmitting()"
                  class="p-2 min-w-[36px] min-h-[36px] bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white rounded-full active:scale-95 flex items-center justify-center flex-shrink-0">
                  <app-icon name="send" [size]="16"></app-icon>
                </button>
              </div>
              <button 
                (click)="cancelReply()"
                class="mt-1 text-xs text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 px-2 py-1">
                Cancel
              </button>
            </div>
          </div>
        }

        <!-- Replies -->
        @if (comment().replies && comment().replies!.length > 0) {
          <div class="mt-3">
            @for (reply of comment().replies; track reply.id) {
              <app-comment-item 
                [comment]="reply" 
                [postId]="postId()"
                [isReply]="true"
                (commentUpdated)="onReplyUpdated()"
              ></app-comment-item>
            }
          </div>
        }
      </div>
    </div>
  `
})
export class CommentItemComponent {
  comment = input.required<Comment>();
  postId = input.required<string>();
  isReply = input<boolean>(false);
  commentUpdated = output<void>();

  private commentService = inject(CommentService);
  authService = inject(AuthService);
  private textParser = inject(TextParserService);

  showReplyInput = signal(false);
  replyText = signal('');
  isSubmitting = signal(false);
  isEditing = signal(false);
  editText = signal('');
  currentReaction = signal<ReactionType | null>(null);

  menuItems = signal<MenuItem[]>([]);

  ngOnInit() {
    this.updateMenuItems();
    this.loadUserReaction();
  }

  async loadUserReaction() {
    const userId = this.authService.currentUser()?.id;
    if (!userId) return;

    const { data } = await this.commentService.getUserCommentReaction(this.comment().id, userId);
    this.currentReaction.set(data?.reaction_type || null);
  }

  updateMenuItems() {
    const isOwner = this.isOwner();
    this.menuItems.set([
      { id: 'copy', label: 'Copy link', icon: 'link', show: true },
      { id: 'edit', label: 'Edit comment', icon: 'edit', show: isOwner },
      { id: 'delete', label: 'Delete comment', icon: 'trash', danger: true, show: isOwner },
      { id: 'report', label: 'Report comment', icon: 'flag', danger: true, show: !isOwner },
    ]);
  }

  isOwner() {
    return this.authService.currentUser()?.id === this.comment().user_id;
  }

  getReactionEmoji(type: ReactionType | null): string {
    const reactions: Record<ReactionType, string> = {
      'LIKE': '👍',
      'LOVE': '❤️',
      'HAHA': '😂',
      'WOW': '😮',
      'SAD': '😢',
      'ANGRY': '😠'
    };
    return type ? reactions[type] : '';
  }

  getReactionColor(type: ReactionType | null): string {
    const colors: Record<ReactionType, string> = {
      'LIKE': 'text-blue-500',
      'LOVE': 'text-red-500',
      'HAHA': 'text-yellow-500',
      'WOW': 'text-yellow-500',
      'SAD': 'text-blue-400',
      'ANGRY': 'text-orange-500'
    };
    return type ? colors[type] : '';
  }

  toggleReply() {
    this.showReplyInput.update(v => !v);
    if (!this.showReplyInput()) {
      this.replyText.set('');
    }
  }

  autoResizeReply(event: Event) {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 80) + 'px';
  }

  async submitReply() {
    if (!this.replyText().trim() || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    try {
      await this.commentService.createComment(
        this.postId(),
        this.replyText(),
        this.comment().id
      );
      this.replyText.set('');
      this.showReplyInput.set(false);
      this.commentUpdated.emit();
    } catch (err) {
      console.error('Error posting reply:', err);
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancelReply() {
    this.showReplyInput.set(false);
    this.replyText.set('');
  }

  async handleReaction(type: ReactionType) {
    const current = this.currentReaction();
    const commentId = this.comment().id;
    const userId = this.authService.currentUser()?.id;
    
    if (!userId) return;

    try {
      if (current === type) {
        // Remove reaction
        await this.commentService.removeCommentReaction(commentId, userId);
        this.currentReaction.set(null);
        this.comment().likes_count = Math.max(0, this.comment().likes_count - 1);
      } else if (current) {
        // Change reaction (no count change)
        await this.commentService.updateCommentReaction(commentId, userId, type);
        this.currentReaction.set(type);
      } else {
        // Add new reaction
        await this.commentService.addCommentReaction(commentId, userId, type);
        this.currentReaction.set(type);
        this.comment().likes_count = this.comment().likes_count + 1;
      }
      
      this.commentUpdated.emit();
    } catch (err) {
      console.error('Error saving comment reaction:', err);
    }
  }

  handleMenuAction(action: string) {
    switch (action) {
      case 'copy':
        const url = `${window.location.origin}/app/post/${this.postId()}#comment-${this.comment().id}`;
        navigator.clipboard.writeText(url);
        break;
      case 'edit':
        this.startEdit();
        break;
      case 'delete':
        this.deleteComment();
        break;
      case 'report':
        if (confirm('Report this comment?')) {
          console.log('Report comment');
        }
        break;
    }
  }

  startEdit() {
    this.isEditing.set(true);
    this.editText.set(this.comment().content);
  }

  cancelEdit() {
    this.isEditing.set(false);
    this.editText.set('');
  }

  async saveEdit() {
    if (!this.editText().trim()) return;

    try {
      await this.commentService.editComment(this.comment().id, this.editText());
      this.comment().content = this.editText();
      this.comment().is_edited = true;
      this.isEditing.set(false);
      this.commentUpdated.emit();
    } catch (err) {
      console.error('Error editing comment:', err);
    }
  }

  async deleteComment() {
    if (!confirm('Delete this comment?')) return;

    try {
      await this.commentService.deleteComment(this.comment().id);
      this.commentUpdated.emit();
    } catch (err) {
      console.error('Error deleting comment:', err);
    }
  }

  onReplyUpdated() {
    this.commentUpdated.emit();
  }

  formatDate(dateStr: string) {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = (now.getTime() - date.getTime()) / 1000;

    if (diff < 60) return 'just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    if (diff < 604800) return `${Math.floor(diff / 86400)}d`;
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }

  parseText(text: string) {
    return this.textParser.parseText(text);
  }

  handleMentionClick(username: string) {
    console.log('Mention clicked:', username);
  }

  handleHashtagClick(tag: string) {
    console.log('Hashtag clicked:', tag);
  }
}
