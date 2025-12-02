import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inbox-shimmer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-4 flex gap-3 items-center animate-pulse">
      <div class="w-12 h-12 bg-slate-200 dark:bg-slate-800 rounded-full"></div>
      <div class="flex-1">
        <div class="h-4 bg-slate-200 dark:bg-slate-800 rounded w-32 mb-2"></div>
        <div class="h-3 bg-slate-200 dark:bg-slate-800 rounded w-48"></div>
      </div>
      <div class="h-3 bg-slate-200 dark:bg-slate-800 rounded w-12"></div>
    </div>
  `
})
export class InboxShimmerComponent {}
