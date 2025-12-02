
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../components/icon.component';
import { NewChatDialogComponent } from '../components/new-chat-dialog.component';
import { ChatBubbleShimmerComponent } from '../components/ui/shimmer.component';
import { InboxShimmerComponent } from '../components/inbox-shimmer.component';
import { MessagingService, Chat, Message } from '../services/messaging.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, IconComponent, FormsModule, NewChatDialogComponent, ChatBubbleShimmerComponent, InboxShimmerComponent],
  template: `
    @if (showNewChatDialog()) {
      <app-new-chat-dialog (close)="showNewChatDialog.set(false)" (userSelected)="startNewChat($event)"></app-new-chat-dialog>
    }

    <!-- Message Actions Bottom Sheet -->
    @if (showMessageActions()) {
      <div class="fixed inset-0 z-50" (click)="closeMessageActions()">
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
        <div class="absolute bottom-0 left-0 right-0 bg-white dark:bg-slate-900 rounded-t-3xl p-4 pb-8 transform transition-transform duration-300" (click)="$event.stopPropagation()">
          <div class="w-12 h-1 bg-slate-300 dark:bg-slate-700 rounded-full mx-auto mb-4"></div>
          
          @if (selectedMessage(); as msg) {
            <div class="space-y-1">
              @if (!msg.is_deleted) {
                <!-- Copy -->
                <button (click)="copyMessage(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="copy" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Copy message</span>
                </button>
                
                <!-- Reply -->
                <button (click)="replyToMessage(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="reply" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Reply</span>
                </button>
              }
              
              @if (msg.is_me && !msg.is_deleted) {
                <!-- Edit (sender only) -->
                <button (click)="editMessageFromSheet(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="edit" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Edit message</span>
                </button>
                
                <!-- Edit History (sender only, if edited) -->
                @if (msg.is_edited) {
                  <button (click)="showEditHistory(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                    <app-icon name="clock" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                    <span class="text-slate-900 dark:text-white font-medium">See edit history</span>
                  </button>
                }
                
                <!-- Delete for me -->
                <button (click)="deleteForMe(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="trash" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Delete for me</span>
                </button>
                
                <!-- Delete for everyone (sender only) -->
                <button (click)="deleteForEveryone(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors">
                  <app-icon name="trash" [size]="20" class="text-red-500"></app-icon>
                  <span class="text-red-500 font-medium">Delete for everyone</span>
                </button>
              } @else if (!msg.is_deleted) {
                <!-- Delete for me (others' messages) -->
                <button (click)="deleteForMe(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="trash" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Delete for me</span>
                </button>
                
                <!-- Report (others' messages) -->
                <button (click)="reportMessage(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors">
                  <app-icon name="flag" [size]="20" class="text-red-500"></app-icon>
                  <span class="text-red-500 font-medium">Report message</span>
                </button>
              }
              
              @if (msg.is_deleted) {
                <!-- Delete for me (for deleted messages) -->
                <button (click)="deleteForMe(msg)" class="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
                  <app-icon name="trash" [size]="20" class="text-slate-600 dark:text-slate-400"></app-icon>
                  <span class="text-slate-900 dark:text-white font-medium">Remove from chat</span>
                </button>
              }
            </div>
          }
          
          <button (click)="closeMessageActions()" class="w-full mt-4 py-3 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-white font-bold">
            Cancel
          </button>
        </div>
      </div>
    }
    
    <div class="flex h-[100dvh] overflow-hidden bg-white dark:bg-slate-950">
      <!-- Chat List (Sidebar) -->
      <div class="w-full md:w-80 lg:w-96 flex-shrink-0 border-r border-slate-200 dark:border-white/10 flex flex-col bg-white dark:bg-slate-950"
           [class.hidden]="activeChat() && isMobile">
         
         <div class="p-3 md:p-4 border-b border-slate-200 dark:border-white/10 flex items-center gap-3 sticky top-0 bg-white/80 dark:bg-slate-950/80 backdrop-blur z-10">
            <a href="#/app/feed" class="p-2 -ml-2 rounded-full hover:bg-slate-100 dark:hover:bg-white/10 text-slate-600 dark:text-slate-400">
               <app-icon name="chevron-left" [size]="20"></app-icon>
            </a>
            <h1 class="text-lg md:text-xl font-bold text-slate-900 dark:text-white flex-1">Messages</h1>
            <button (click)="showNewChatDialog.set(true)" class="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-white/10 text-slate-600 dark:text-slate-400">
               <app-icon name="plus" [size]="20"></app-icon>
            </button>
         </div>

         <div class="p-3 md:p-4">
            <div class="relative">
              <app-icon name="search" [size]="18" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"></app-icon>
              <input 
                type="text" 
                [(ngModel)]="searchQuery"
                placeholder="Search conversations" 
                class="w-full pl-10 pr-10 py-2 rounded-full bg-slate-100 dark:bg-slate-900 border-none focus:ring-2 focus:ring-indigo-500 outline-none text-sm dark:text-white">
              @if (searchQuery()) {
                <button (click)="searchQuery.set('')" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300">
                  <app-icon name="x" [size]="16"></app-icon>
                </button>
              }
            </div>
         </div>

         <div class="flex-1 overflow-y-auto">
            @if (messagingService.loadingChats()) {
              @for (i of [1,2,3,4,5]; track i) {
                <app-inbox-shimmer></app-inbox-shimmer>
              }
            } @else if (filteredChats().length === 0 && searchQuery()) {
              <div class="p-8 text-center">
                <app-icon name="search" [size]="48" class="mx-auto mb-3 text-slate-300 dark:text-slate-700"></app-icon>
                <p class="text-slate-500 text-sm">No conversations found</p>
              </div>
            } @else {
              @for (chat of filteredChats(); track chat.id) {
               @if (getPartner(chat); as partner) {
                 <div (click)="selectChat(chat)" 
                      class="p-3 md:p-4 hover:bg-slate-50 dark:hover:bg-white/5 cursor-pointer transition-colors flex gap-2 md:gap-3 items-center border-r-4 border-transparent"
                      [class.border-indigo-500]="activeChat()?.chat_id === chat.chat_id"
                      [class.bg-slate-50]="activeChat()?.chat_id === chat.chat_id"
                      [class.dark:bg-white/5]="activeChat()?.chat_id === chat.chat_id">
                    
                    <div class="relative flex-shrink-0">
                      <img [src]="partner.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + partner.username" class="w-11 h-11 md:w-12 md:h-12 rounded-full object-cover">
                      @if (partner.is_online) {
                        <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white dark:border-slate-950"></div>
                      }
                    </div>
                    
                    <div class="flex-1 min-w-0">
                       <div class="flex justify-between items-center mb-1">
                          <span class="font-bold text-sm md:text-base text-slate-900 dark:text-white truncate flex items-center gap-1">
                            {{ partner.display_name || partner.username }}
                          </span>
                          <span class="text-[11px] md:text-xs text-slate-500 flex-shrink-0 ml-2">{{ chat.last_message_time | date:'short' }}</span>
                       </div>
                       <div class="flex justify-between items-center gap-2">
                          <span class="text-xs md:text-sm text-slate-500 truncate" [class.font-bold]="chat.unread_count > 0" [class.text-slate-900]="chat.unread_count > 0" [class.dark:text-white]="chat.unread_count > 0">
                            {{ chat.last_message || 'No messages yet' }}
                          </span>
                          @if (chat.unread_count > 0) {
                            <div class="bg-indigo-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center flex-shrink-0">
                               {{ chat.unread_count }}
                            </div>
                          }
                       </div>
                    </div>
                 </div>
               }
            }
            }
         </div>
      </div>

      <!-- Chat Window -->
      <div class="flex-1 flex flex-col bg-white dark:bg-slate-950 w-full"
           [class.hidden]="!activeChat() && isMobile">
        
        @if (activeChat()) {
          <!-- Chat Header -->
          <div class="p-2 md:p-3 border-b border-slate-200 dark:border-white/10 flex items-center gap-2 md:gap-4 sticky top-0 bg-white/80 dark:bg-slate-950/80 backdrop-blur z-10">
             <button (click)="backToList()" class="md:hidden p-2 -ml-1 text-slate-600 dark:text-slate-400">
                <app-icon name="chevron-left" [size]="20"></app-icon>
             </button>
             @if (getPartner(activeChat()!); as partner) {
               <div class="flex items-center gap-2 md:gap-3 min-w-0 flex-1">
                  <img [src]="partner.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + partner.username" class="w-9 h-9 md:w-10 md:h-10 rounded-full object-cover flex-shrink-0">
                  <div class="min-w-0 flex-1">
                     <div class="font-bold text-sm md:text-base text-slate-900 dark:text-white flex items-center gap-1 truncate">
                       {{ partner.display_name || partner.username }}
                     </div>
                     <div class="text-xs text-slate-500 truncate">
                        {{ partner.is_online ? 'Online' : '@' + partner.username }}
                     </div>
                  </div>
               </div>
             }
             <div class="ml-auto relative flex-shrink-0">
                <button (click)="showChatMenu.set(!showChatMenu())" class="p-1.5 md:p-2 rounded-full hover:bg-slate-100 dark:hover:bg-white/10 text-indigo-500">
                   <app-icon name="more-horizontal" [size]="20"></app-icon>
                </button>
                @if (showChatMenu()) {
                  <div class="absolute right-0 mt-2 w-48 bg-white dark:bg-slate-800 rounded-lg shadow-lg border border-slate-200 dark:border-slate-700 py-1 z-50">
                    <button (click)="toggleFullscreen()" class="w-full px-4 py-2 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2">
                      <app-icon name="maximize" [size]="16"></app-icon>
                      Go Fullscreen
                    </button>
                  </div>
                }
             </div>
          </div>

          <!-- Messages Area -->
          <div class="flex-1 overflow-y-auto p-2 md:p-4 space-y-1">
             @if (messagingService.loadingMessages()) {
               @for (i of [1,2,3,4,5]; track i) {
                 <app-chat-bubble-shimmer [isOwn]="i % 2 === 0"></app-chat-bubble-shimmer>
               }
             } @else {
               @for (msg of messages(); track msg.id; let i = $index) {
                @let prevMsg = i > 0 ? messages()[i - 1] : null;
                @let nextMsg = i < messages().length - 1 ? messages()[i + 1] : null;
                @let isGroupStart = isMessageGroupStart(msg, prevMsg);
                @let isGroupEnd = isMessageGroupEnd(msg, nextMsg);
                
                <div class="flex gap-1.5 md:gap-2 group" [class.flex-row-reverse]="msg.is_me" [class.mt-3]="isGroupStart" [class.md:mt-4]="isGroupStart">
                   <!-- Avatar (only on group start) -->
                   @if (!msg.is_me && isGroupStart) {
                     <div class="flex-shrink-0 w-6 md:w-8">
                        @if (msg.sender) {
                          <img [src]="msg.sender.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + msg.sender.username" class="w-6 h-6 md:w-8 md:h-8 rounded-full object-cover">
                        }
                     </div>
                   } @else if (!msg.is_me) {
                     <div class="w-6 md:w-8"></div>
                   }
                   
                   <div class="max-w-[80%] md:max-w-[70%] flex items-end gap-1 md:gap-2" [class.flex-row-reverse]="msg.is_me">
                      <!-- Message Bubble with long press -->
                      <div class="relative"
                           (touchstart)="onMessageTouchStart($event, msg)"
                           (touchend)="onMessageTouchEnd()"
                           (touchmove)="onMessageTouchEnd()"
                           (contextmenu)="onMessageContextMenu($event, msg)">
                        <div class="px-2.5 py-1.5 md:px-3 md:py-2 rounded-2xl text-sm break-words select-none"
                             [class.bg-indigo-600]="msg.is_me && !msg.is_deleted"
                             [class.text-white]="msg.is_me && !msg.is_deleted"
                             [class.bg-slate-200]="msg.is_deleted"
                             [class.dark:bg-slate-800]="msg.is_deleted"
                             [class.italic]="msg.is_deleted"
                             [class.text-slate-500]="msg.is_deleted"
                             [class.dark:text-slate-500]="msg.is_deleted"
                             [class.rounded-tr-sm]="msg.is_me && !isGroupEnd"
                             [class.rounded-br-sm]="msg.is_me && !isGroupStart"
                             [class.bg-slate-100]="!msg.is_me && !msg.is_deleted"
                             [class.dark:bg-slate-800]="!msg.is_me && !msg.is_deleted"
                             [class.text-slate-900]="!msg.is_me && !msg.is_deleted"
                             [class.dark:text-white]="!msg.is_me && !msg.is_deleted"
                             [class.rounded-tl-sm]="!msg.is_me && !isGroupEnd"
                             [class.rounded-bl-sm]="!msg.is_me && !isGroupStart"
                             [class.ring-2]="selectedMessage()?.id === msg.id"
                             [class.ring-indigo-400]="selectedMessage()?.id === msg.id">
                           @if (msg.message_type === 'image' && msg.media_url && !msg.is_deleted) {
                             <img [src]="msg.media_url" class="max-w-xs rounded-lg mb-1" alt="Image">
                           }
                           @if (msg.message_type === 'video' && msg.media_url && !msg.is_deleted) {
                             <video [src]="msg.media_url" controls class="max-w-xs rounded-lg mb-1"></video>
                           }
                           @if (msg.message_type === 'audio' && msg.media_url && !msg.is_deleted) {
                             <audio [src]="msg.media_url" controls class="mb-1"></audio>
                           }
                           @if (msg.message_type === 'file' && msg.media_url && !msg.is_deleted) {
                             <a [href]="msg.media_url" target="_blank" class="flex items-center gap-2 p-2 bg-white/10 rounded mb-1">
                               <app-icon name="file" [size]="20"></app-icon>
                               <span class="text-xs">{{ msg.content }}</span>
                             </a>
                           }
                           <div class="flex items-center gap-1">
                             @if (msg.is_deleted) {
                               <app-icon name="trash" [size]="12" class="opacity-50"></app-icon>
                             }
                             <span>{{ msg.content }}</span>
                           </div>
                           <div class="text-[10px] mt-0.5 md:mt-1 opacity-70 flex items-center gap-1 justify-end">
                              <span>{{ msg.created_at | date:'shortTime' }}</span>
                              @if (msg.is_edited) {
                                <span class="italic">edited</span>
                              }
                              @if (msg.is_me) {
                                <span>{{ msg.delivery_status === 'read' ? '✓✓' : '✓' }}</span>
                              }
                           </div>
                        </div>
                      </div>
                   </div>
                </div>
               }
             }
          </div>

          <!-- Input Area -->
          <div class="p-2 md:p-4 border-t border-slate-200 dark:border-white/10 bg-white dark:bg-slate-950">
             <div class="flex items-center gap-1 md:gap-2 bg-slate-100 dark:bg-slate-900 rounded-2xl px-1.5 md:px-2 py-1">
                <input type="file" multiple #fileInput (change)="handleFileSelect($event)" class="hidden" accept="image/*,video/*,audio/*,.pdf,.doc,.docx">
                <button (click)="fileInput.click()" [disabled]="uploading()" class="p-1.5 md:p-2 text-indigo-500 hover:bg-indigo-500/10 rounded-full transition-colors disabled:opacity-50">
                   <app-icon [name]="uploading() ? 'loader' : 'image'" [size]="18"></app-icon>
                </button>
                @if (!isRecording()) {
                  <button (click)="startVoiceRecording()" class="p-1.5 md:p-2 text-indigo-500 hover:bg-indigo-500/10 rounded-full transition-colors">
                     <app-icon name="mic" [size]="18"></app-icon>
                  </button>
                } @else {
                  <button (click)="stopVoiceRecording()" class="p-1.5 md:p-2 text-red-500 hover:bg-red-500/10 rounded-full transition-colors animate-pulse">
                     <app-icon name="square" [size]="18"></app-icon>
                  </button>
                }
                <input 
                  type="text" 
                  autocomplete="off"
                  placeholder="Type a message" 
                  [(ngModel)]="messageInput"
                  (keyup.enter)="sendMessage()"
                  (input)="onTyping()"
                  (blur)="onStopTyping()"
                  class="flex-1 bg-transparent border-none focus:ring-0 text-sm dark:text-white h-9 md:h-10">
                <button 
                  (click)="sendMessage()"
                  [disabled]="!_messageInput().trim() || uploading()"
                  class="p-1.5 md:p-2 text-indigo-500 hover:bg-indigo-500/10 rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                   <app-icon name="send" [size]="18"></app-icon>
                </button>
             </div>
          </div>
        } @else {
           <div class="flex-1 flex flex-col items-center justify-center text-slate-500 p-8 text-center">
              <div class="w-24 h-24 bg-slate-100 dark:bg-slate-900 rounded-full flex items-center justify-center mb-6">
                 <app-icon name="mail" [size]="48" class="opacity-50"></app-icon>
              </div>
              <h2 class="text-2xl font-bold text-slate-900 dark:text-white mb-2">Select a message</h2>
              <p>Choose from your existing conversations or start a new one.</p>
              <button (click)="showNewChatDialog.set(true)" class="mt-8 px-6 py-3 bg-indigo-600 text-white rounded-full font-bold hover:bg-indigo-500">
                 New Message
              </button>
           </div>
        }
      </div>
    </div>
  `
})
export class MessagesComponent implements OnInit, OnDestroy {
  messagingService = inject(MessagingService);
  chats = this.messagingService.chats;
  messages = this.messagingService.messages;
  
  activeChat = signal<Chat | null>(null);
  isMobile = false;
  private _messageInput = signal('');
  showNewChatDialog = signal(false);
  searchQuery = signal('');
  showChatMenu = signal(false);
  uploading = signal(false);
  isRecording = signal(false);
  showMessageActions = signal(false);
  selectedMessage = signal<Message | null>(null);
  private longPressTimer: any = null;
  mediaRecorder?: MediaRecorder;
  audioChunks: Blob[] = [];
  
  get messageInput() {
    return this._messageInput();
  }
  
  set messageInput(value: string) {
    this._messageInput.set(value);
  }

  constructor() {
    this.checkScreen();
    window.addEventListener('resize', () => this.checkScreen());
  }

  async ngOnInit() {
    await this.messagingService.fetchChats();
  }

  ngOnDestroy() {
    this.messagingService.cleanup();
  }

  checkScreen() {
    this.isMobile = window.innerWidth < 768;
  }

  async selectChat(chat: Chat) {
    this.activeChat.set(chat);
    await this.messagingService.fetchMessages(chat.chat_id);
    this.messagingService.setupRealtimeMessages(chat.chat_id);
    this.messagingService.setupPresence(chat.chat_id);
  }

  backToList() {
    this.activeChat.set(null);
    this.messagingService.cleanup();
  }

  async sendMessage() {
    const content = this._messageInput().trim();
    if (!content || !this.activeChat()) return;

    try {
      await this.messagingService.sendMessage(this.activeChat()!.chat_id, content);
      this._messageInput.set('');
    } catch (err) {
      console.error('Failed to send message:', err);
    }
  }

  onTyping() {
    if (this.activeChat()) {
      this.messagingService.setTypingStatus(this.activeChat()!.chat_id, true);
    }
  }

  onStopTyping() {
    if (this.activeChat()) {
      this.messagingService.setTypingStatus(this.activeChat()!.chat_id, false);
    }
  }

  getPartner(chat: Chat | null) {
    if (!chat) return null;
    const currentUserId = this.messagingService.auth.currentUser()?.id;
    return chat.participants.find(p => p.uid !== currentUserId) || null;
  }

  async startNewChat(user: any) {
    try {
      const chatId = await this.messagingService.createChat([user.uid]);
      if (chatId) {
        await this.messagingService.fetchChats();
        const newChat = this.chats().find(c => c.chat_id === chatId);
        if (newChat) {
          this.selectChat(newChat);
        }
      }
    } catch (err) {
      console.error('Failed to create chat:', err);
    }
  }

  filteredChats() {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.chats();
    
    const currentUserId = this.messagingService.auth.currentUser()?.id;
    return this.chats().filter(chat => {
      const partner = chat.participants.find(p => p.uid !== currentUserId);
      if (!partner) return false;
      
      const displayName = (partner.display_name || '').toLowerCase();
      const username = (partner.username || '').toLowerCase();
      const lastMessage = (chat.last_message || '').toLowerCase();
      
      return displayName.includes(query) || 
             username.includes(query) || 
             lastMessage.includes(query);
    });
  }

  editMessage(msg: Message) {
    const newContent = prompt('Edit message:', msg.content);
    if (newContent && newContent.trim() && newContent !== msg.content) {
      this.messagingService.editMessage(msg.id, newContent.trim());
    }
  }

  async deleteMessage(msg: Message) {
    if (confirm('Delete this message?')) {
      await this.messagingService.deleteMessage(msg.id);
    }
  }

  // Long press handlers for message actions
  onMessageTouchStart(event: TouchEvent, msg: Message) {
    this.longPressTimer = setTimeout(() => {
      this.selectedMessage.set(msg);
      this.showMessageActions.set(true);
      if (navigator.vibrate) navigator.vibrate(50);
    }, 500);
  }

  onMessageTouchEnd() {
    if (this.longPressTimer) {
      clearTimeout(this.longPressTimer);
      this.longPressTimer = null;
    }
  }

  onMessageContextMenu(event: Event, msg: Message) {
    event.preventDefault();
    this.selectedMessage.set(msg);
    this.showMessageActions.set(true);
  }

  closeMessageActions() {
    this.showMessageActions.set(false);
    this.selectedMessage.set(null);
  }

  copyMessage(msg: Message) {
    navigator.clipboard.writeText(msg.content);
    this.closeMessageActions();
  }

  replyToMessage(msg: Message) {
    this._messageInput.set(`> ${msg.content}\n`);
    this.closeMessageActions();
  }

  editMessageFromSheet(msg: Message) {
    this.closeMessageActions();
    const newContent = prompt('Edit message:', msg.content);
    if (newContent && newContent.trim() && newContent !== msg.content) {
      this.messagingService.editMessage(msg.id, newContent.trim())
        .catch(err => {
          alert('Failed to edit message: ' + (err.message || 'Unknown error'));
        });
    }
  }

  async deleteMessageFromSheet(msg: Message) {
    this.closeMessageActions();
    if (confirm('Delete this message?')) {
      await this.messagingService.deleteMessage(msg.id);
    }
  }

  async deleteForMe(msg: Message) {
    this.closeMessageActions();
    await this.messagingService.deleteMessageForMe(msg.id);
  }

  async deleteForEveryone(msg: Message) {
    this.closeMessageActions();
    if (confirm('Delete this message for everyone? This cannot be undone.')) {
      await this.messagingService.deleteMessageForEveryone(msg.id);
    }
  }

  async showEditHistory(msg: Message) {
    this.closeMessageActions();
    const history = await this.messagingService.getEditHistory(msg.id);
    if (history.length === 0) {
      alert('No edit history available');
      return;
    }
    const historyText = history.map((h: any, i: number) => 
      `${i + 1}. "${h.content}" (${new Date(h.edited_at).toLocaleString()})`
    ).join('\n\n');
    alert(`Edit History:\n\n${historyText}`);
  }

  reportMessage(msg: Message) {
    this.closeMessageActions();
    if (confirm('Report this message for inappropriate content?')) {
      alert('Message reported. Thank you for helping keep our community safe.');
    }
  }

  isMessageGroupStart(msg: Message, prevMsg: Message | null): boolean {
    if (!prevMsg) return true;
    if (prevMsg.sender_id !== msg.sender_id) return true;
    const timeDiff = new Date(msg.created_at).getTime() - new Date(prevMsg.created_at).getTime();
    return timeDiff > 300000; // 5 minutes
  }

  isMessageGroupEnd(msg: Message, nextMsg: Message | null): boolean {
    if (!nextMsg) return true;
    if (nextMsg.sender_id !== msg.sender_id) return true;
    const timeDiff = new Date(nextMsg.created_at).getTime() - new Date(msg.created_at).getTime();
    return timeDiff > 300000; // 5 minutes
  }

  toggleFullscreen() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
    } else {
      document.exitFullscreen();
    }
    this.showChatMenu.set(false);
  }

  async handleFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files || files.length === 0 || !this.activeChat()) return;

    this.uploading.set(true);
    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const url = await this.messagingService.uploadMedia(file);
        const type = file.type.startsWith('image/') ? 'image' : 
                     file.type.startsWith('video/') ? 'video' : 
                     file.type.startsWith('audio/') ? 'audio' : 'file';
        
        await this.messagingService.sendMessage(
          this.activeChat()!.chat_id,
          file.name,
          type,
          url,
          file.type,
          file.size
        );
      }
    } catch (err) {
      console.error('Upload failed:', err);
      alert('Failed to upload file');
    } finally {
      this.uploading.set(false);
      input.value = '';
    }
  }

  async startVoiceRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream);
      this.audioChunks = [];

      this.mediaRecorder.ondataavailable = (e) => {
        this.audioChunks.push(e.data);
      };

      this.mediaRecorder.onstop = async () => {
        const blob = new Blob(this.audioChunks, { type: 'audio/webm' });
        const file = new File([blob], 'voice.webm', { type: 'audio/webm' });
        
        this.uploading.set(true);
        try {
          const url = await this.messagingService.uploadMedia(file);
          await this.messagingService.sendMessage(
            this.activeChat()!.chat_id,
            'Voice message',
            'audio',
            url,
            'audio/webm',
            blob.size
          );
        } catch (err) {
          console.error('Voice upload failed:', err);
        } finally {
          this.uploading.set(false);
        }
        
        stream.getTracks().forEach(track => track.stop());
      };

      this.mediaRecorder.start();
      this.isRecording.set(true);
    } catch (err) {
      console.error('Microphone access denied:', err);
      alert('Microphone access required');
    }
  }

  stopVoiceRecording() {
    if (this.mediaRecorder && this.isRecording()) {
      this.mediaRecorder.stop();
      this.isRecording.set(false);
    }
  }
}
