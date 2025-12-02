import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-shimmer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="'animate-pulse ' + className">
      <div class="bg-gray-200 dark:bg-gray-700 rounded" [style.height]="height" [style.width]="width"></div>
    </div>
  `
})
export class ShimmerComponent {
  @Input() height = '1rem';
  @Input() width = '100%';
  @Input() className = '';
}

@Component({
  selector: 'app-post-shimmer',
  standalone: true,
  imports: [CommonModule, ShimmerComponent],
  template: `
    <div class="bg-white dark:bg-gray-800 rounded-lg p-4 mb-4">
      <div class="flex items-start gap-3 mb-3">
        <app-shimmer height="40px" width="40px" className="rounded-full"></app-shimmer>
        <div class="flex-1">
          <app-shimmer height="16px" width="120px" className="mb-2"></app-shimmer>
          <app-shimmer height="12px" width="80px"></app-shimmer>
        </div>
      </div>
      <app-shimmer height="14px" width="100%" className="mb-2"></app-shimmer>
      <app-shimmer height="14px" width="90%"></app-shimmer>
    </div>
  `
})
export class PostShimmerComponent {}

@Component({
  selector: 'app-comment-shimmer',
  standalone: true,
  imports: [CommonModule, ShimmerComponent],
  template: `
    <div class="flex gap-2 mb-3">
      <app-shimmer height="32px" width="32px" className="rounded-full"></app-shimmer>
      <div class="flex-1">
        <app-shimmer height="12px" width="100px" className="mb-2"></app-shimmer>
        <app-shimmer height="14px" width="100%"></app-shimmer>
      </div>
    </div>
  `
})
export class CommentShimmerComponent {}

@Component({
  selector: 'app-message-shimmer',
  standalone: true,
  imports: [CommonModule, ShimmerComponent],
  template: `
    <div class="flex items-start gap-3 p-3 hover:bg-gray-50 dark:hover:bg-gray-800">
      <app-shimmer height="48px" width="48px" className="rounded-full"></app-shimmer>
      <div class="flex-1">
        <app-shimmer height="16px" width="140px" className="mb-2"></app-shimmer>
        <app-shimmer height="14px" width="200px"></app-shimmer>
      </div>
    </div>
  `
})
export class MessageShimmerComponent {}

@Component({
  selector: 'app-chat-bubble-shimmer',
  standalone: true,
  imports: [CommonModule, ShimmerComponent],
  template: `
    <div class="flex gap-2 mb-4" [class.justify-end]="isOwn">
      <app-shimmer *ngIf="!isOwn" height="32px" width="32px" className="rounded-full"></app-shimmer>
      <app-shimmer 
        height="40px" 
        [width]="isOwn ? '200px' : '250px'"
        [className]="'rounded-2xl ' + (isOwn ? 'rounded-br-sm' : 'rounded-bl-sm')">
      </app-shimmer>
    </div>
  `
})
export class ChatBubbleShimmerComponent {
  @Input() isOwn = false;
}
