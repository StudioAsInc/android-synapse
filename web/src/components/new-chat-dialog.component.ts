import { Component, inject, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from './icon.component';
import { SupabaseService } from '../services/supabase.service';

@Component({
  selector: 'app-new-chat-dialog',
  standalone: true,
  imports: [CommonModule, IconComponent, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" (click)="close.emit()">
      <div class="bg-white dark:bg-slate-900 rounded-2xl w-full max-w-md mx-4 shadow-xl" (click)="$event.stopPropagation()">
        <div class="p-4 border-b border-slate-200 dark:border-white/10 flex justify-between items-center">
          <h2 class="text-xl font-bold text-slate-900 dark:text-white">New Message</h2>
          <button (click)="close.emit()" class="p-2 hover:bg-slate-100 dark:hover:bg-white/10 rounded-full">
            <app-icon name="x" [size]="20"></app-icon>
          </button>
        </div>

        <div class="p-4">
          <input 
            type="text" 
            placeholder="Search users..." 
            [(ngModel)]="searchQuery"
            (input)="searchUsers()"
            class="w-full px-4 py-2 rounded-full bg-slate-100 dark:bg-slate-800 border-none focus:ring-2 focus:ring-indigo-500 outline-none text-sm dark:text-white">
        </div>

        <div class="max-h-96 overflow-y-auto">
          @for (user of users(); track user.uid) {
            <div (click)="selectUser(user)" 
                 class="p-4 hover:bg-slate-50 dark:hover:bg-white/5 cursor-pointer flex items-center gap-3">
              <img [src]="user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.username" 
                   class="w-12 h-12 rounded-full object-cover">
              <div>
                <div class="font-bold text-slate-900 dark:text-white">{{ user.display_name || user.username }}</div>
                <div class="text-sm text-slate-500">@{{ user.username }}</div>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class NewChatDialogComponent {
  private supabase = inject(SupabaseService).client;
  
  searchQuery = '';
  users = signal<any[]>([]);
  close = output<void>();
  userSelected = output<any>();

  async searchUsers() {
    if (!this.searchQuery.trim()) {
      this.users.set([]);
      return;
    }

    const { data } = await this.supabase
      .from('users')
      .select('uid, username, display_name, avatar')
      .or(`username.ilike.%${this.searchQuery}%,display_name.ilike.%${this.searchQuery}%`)
      .limit(10);

    this.users.set(data || []);
  }

  selectUser(user: any) {
    this.userSelected.emit(user);
    this.close.emit();
  }
}
