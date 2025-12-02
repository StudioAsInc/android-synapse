import { Injectable, signal, inject } from '@angular/core';
import { SupabaseService } from './supabase.service';
import { AuthService } from './auth.service';
import { RealtimeChannel } from '@supabase/supabase-js';

export interface ChatUser {
  uid: string;
  username: string;
  display_name: string;
  avatar: string;
  is_online?: boolean;
}

export interface Chat {
  id: string;
  chat_id: string;
  is_group: boolean;
  chat_name?: string;
  chat_avatar?: string;
  last_message?: string;
  last_message_time?: string;
  participants: ChatUser[];
  unread_count: number;
}

export interface Message {
  id: string;
  chat_id: string;
  sender_id: string;
  content: string;
  message_type: string;
  media_url?: string;
  created_at: string;
  is_me: boolean;
  delivery_status: string;
  sender?: ChatUser;
  is_edited?: boolean;
  is_deleted?: boolean;
  edit_history?: any[];
}

@Injectable({
  providedIn: 'root'
})
export class MessagingService {
  private supabase = inject(SupabaseService).client;
  auth = inject(AuthService);
  
  chats = signal<Chat[]>([]);
  messages = signal<Message[]>([]);
  activeChat = signal<string | null>(null);
  typingUsers = signal<string[]>([]);
  loadingMessages = signal<boolean>(false);
  loadingChats = signal<boolean>(false);
  
  private messageChannel?: RealtimeChannel;
  private presenceChannel?: RealtimeChannel;

  async fetchChats() {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    this.loadingChats.set(true);
    try {
      const { data: participantData, error: participantError } = await this.supabase
        .from('chat_participants')
        .select('chat_id')
        .eq('user_id', userId);

      if (participantError) throw participantError;

      const chatIds = (participantData || []).map((p: any) => p.chat_id);
      
      if (chatIds.length === 0) {
        this.chats.set([]);
        return;
      }

      const { data: chatsData, error: chatsError } = await this.supabase
        .from('chats')
        .select('*')
        .in('chat_id', chatIds);

      if (chatsError) throw chatsError;

      const chatsWithParticipants = await Promise.all(
        (chatsData || []).map(async (chat: any) => {
          const participants = await this.fetchChatParticipants(chat.chat_id);
          const unreadCount = await this.getUnreadCount(chat.chat_id, userId);
          
          return {
            ...chat,
            participants,
            unread_count: unreadCount
          };
        })
      );

      this.chats.set(chatsWithParticipants);
    } catch (err) {
      console.error('Error fetching chats:', err);
    } finally {
      this.loadingChats.set(false);
    }
  }

  async fetchChatParticipants(chatId: string): Promise<ChatUser[]> {
    const { data, error } = await this.supabase
      .from('chat_participants')
      .select('user_id')
      .eq('chat_id', chatId);

    if (error || !data) return [];

    const userIds = data.map((p: any) => p.user_id);
    
    const { data: usersData, error: usersError } = await this.supabase
      .from('users')
      .select('uid, username, display_name, avatar')
      .in('uid', userIds);

    if (usersError || !usersData) return [];

    return usersData.map((user: any) => ({
      uid: user.uid,
      username: user.username,
      display_name: user.display_name,
      avatar: user.avatar
    }));
  }

  async getUnreadCount(chatId: string, userId: string): Promise<number> {
    const { data } = await this.supabase
      .from('chat_participants')
      .select('last_read_message_id')
      .eq('chat_id', chatId)
      .eq('user_id', userId)
      .single();

    if (!data?.last_read_message_id) return 0;

    const { count } = await this.supabase
      .from('messages')
      .select('*', { count: 'exact', head: true })
      .eq('chat_id', chatId)
      .gt('created_at', data.last_read_message_id);

    return count || 0;
  }

  async fetchMessages(chatId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    this.loadingMessages.set(true);
    try {
      const { data, error } = await this.supabase
        .from('messages')
        .select('*')
        .eq('chat_id', chatId)
        .order('created_at', { ascending: true });

      if (error) throw error;

      const senderIds = [...new Set((data || []).map((msg: any) => msg.sender_id))];
      
      const { data: usersData } = await this.supabase
        .from('users')
        .select('uid, username, display_name, avatar')
        .in('uid', senderIds);

      const usersMap = new Map((usersData || []).map((u: any) => [u.uid, u]));

      const messages = (data || [])
        .filter((msg: any) => {
          // Filter out messages deleted for this user
          const deletedForUsers = Array.isArray(msg.deleted_for_users) ? msg.deleted_for_users : [];
          if (deletedForUsers.includes(userId)) return false;
          return true;
        })
        .map((msg: any) => ({
          ...msg,
          is_me: msg.sender_id === userId,
          sender: usersMap.get(msg.sender_id),
          // Show "deleted" placeholder for messages deleted for everyone
          content: (msg.is_deleted || msg.delete_for_everyone) ? 'This message was deleted' : msg.content
        }));

      this.messages.set(messages);
      this.activeChat.set(chatId);
      
      // Mark messages as read
      await this.markAsRead(chatId);
    } catch (err) {
      console.error('Error fetching messages:', err);
    } finally {
      this.loadingMessages.set(false);
    }
  }

  async sendMessage(chatId: string, content: string, messageType: string = 'text', mediaUrl?: string, mediaType?: string, mediaSize?: number) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      const now = new Date().toISOString();
      
      const { error } = await this.supabase
        .from('messages')
        .insert({
          chat_id: chatId,
          sender_id: userId,
          content,
          message_type: messageType,
          media_url: mediaUrl,
          media_type: mediaType,
          media_size: mediaSize,
          delivery_status: 'sent',
          created_at: now,
          message_state: 'sent'
        });

      if (error) throw error;

      const lastMsg = messageType === 'text' ? content : `📎 ${messageType}`;
      await this.supabase
        .from('chats')
        .update({
          last_message: lastMsg,
          last_message_time: now,
          last_message_sender: userId,
          updated_at: now
        })
        .eq('chat_id', chatId);

      await this.fetchMessages(chatId);
    } catch (err) {
      console.error('Error sending message:', err);
      throw err;
    }
  }

  async uploadMedia(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', 'synapse');

    const res = await fetch('https://api.cloudinary.com/v1_1/djw3fgbls/auto/upload', {
      method: 'POST',
      body: formData
    });

    const data = await res.json();
    return data.secure_url;
  }

  async createChat(participantIds: string[], isGroup: boolean = false, chatName?: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      const chatId = `chat_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      
      const { error: chatError } = await this.supabase
        .from('chats')
        .insert({
          chat_id: chatId,
          is_group: isGroup,
          chat_name: chatName,
          created_by: userId,
          participants_count: participantIds.length + 1
        });

      if (chatError) throw chatError;

      // Add participants
      const participants = [userId, ...participantIds].map(uid => ({
        chat_id: chatId,
        user_id: uid,
        role: uid === userId ? 'admin' : 'member'
      }));

      const { error: participantsError } = await this.supabase
        .from('chat_participants')
        .insert(participants);

      if (participantsError) throw participantsError;

      await this.fetchChats();
      return chatId;
    } catch (err) {
      console.error('Error creating chat:', err);
      throw err;
    }
  }

  async markAsRead(chatId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    const lastMessage = this.messages()[this.messages().length - 1];
    if (!lastMessage) return;

    await this.supabase
      .from('chat_participants')
      .update({
        last_read_message_id: lastMessage.id,
        last_read_at: new Date().toISOString()
      })
      .eq('chat_id', chatId)
      .eq('user_id', userId);
  }

  setupRealtimeMessages(chatId: string) {
    this.messageChannel?.unsubscribe();
    
    this.messageChannel = this.supabase
      .channel(`messages:${chatId}`)
      .on('postgres_changes', 
        { event: 'INSERT', schema: 'public', table: 'messages', filter: `chat_id=eq.${chatId}` },
        () => this.fetchMessages(chatId)
      )
      .subscribe();
  }

  setupPresence(chatId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    this.presenceChannel?.unsubscribe();
    
    this.presenceChannel = this.supabase
      .channel(`presence:${chatId}`)
      .on('presence', { event: 'sync' }, () => {
        const state = this.presenceChannel?.presenceState();
        console.log('Presence state:', state);
      })
      .subscribe(async (status) => {
        if (status === 'SUBSCRIBED') {
          await this.presenceChannel?.track({ user_id: userId, online_at: new Date().toISOString() });
        }
      });
  }

  async setTypingStatus(chatId: string, isTyping: boolean) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    await this.supabase
      .from('typing_status')
      .upsert({
        chat_id: chatId,
        user_id: userId,
        is_typing: isTyping,
        timestamp: Date.now()
      });
  }

  async editMessage(messageId: string, newContent: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      // First get the current message to save edit history
      const { data: currentMsg } = await this.supabase
        .from('messages')
        .select('content, edit_history, is_deleted, delete_for_everyone')
        .eq('id', messageId)
        .eq('sender_id', userId)
        .single();

      if (!currentMsg) return;
      
      // Prevent editing deleted messages
      if (currentMsg.is_deleted || currentMsg.delete_for_everyone) {
        throw new Error('Cannot edit deleted messages');
      }
      
      // Don't save if content is the deleted placeholder
      if (currentMsg.content === 'This message was deleted') {
        throw new Error('Cannot edit deleted messages');
      }

      const editHistory = currentMsg.edit_history || [];
      editHistory.push({
        content: currentMsg.content,
        edited_at: new Date().toISOString()
      });

      const { error } = await this.supabase
        .from('messages')
        .update({
          content: newContent,
          is_edited: true,
          edited_at: new Date().toISOString(),
          edit_history: editHistory
        })
        .eq('id', messageId)
        .eq('sender_id', userId);

      if (error) throw error;

      const chatId = this.activeChat();
      if (chatId) await this.fetchMessages(chatId);
    } catch (err) {
      console.error('Error editing message:', err);
      throw err;
    }
  }

  async deleteMessage(messageId: string) {
    // Default to delete for everyone (backward compatibility)
    await this.deleteMessageForEveryone(messageId);
  }

  async deleteMessageForMe(messageId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      // Get current deleted_for_users array
      const { data: msg } = await this.supabase
        .from('messages')
        .select('deleted_for_users')
        .eq('id', messageId)
        .single();

      const deletedForUsers = Array.isArray(msg?.deleted_for_users) ? msg.deleted_for_users : [];
      if (!deletedForUsers.includes(userId)) {
        deletedForUsers.push(userId);
      }

      const { error } = await this.supabase
        .from('messages')
        .update({ deleted_for_users: deletedForUsers })
        .eq('id', messageId);

      if (error) throw error;

      // Remove from local state
      this.messages.update(msgs => msgs.filter(m => m.id !== messageId));
    } catch (err) {
      console.error('Error deleting message for me:', err);
    }
  }

  async deleteMessageForEveryone(messageId: string) {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    try {
      const { error } = await this.supabase
        .from('messages')
        .update({
          is_deleted: true,
          delete_for_everyone: true,
          content: 'This message was deleted'
        })
        .eq('id', messageId)
        .eq('sender_id', userId);

      if (error) throw error;

      const chatId = this.activeChat();
      if (chatId) await this.fetchMessages(chatId);
    } catch (err) {
      console.error('Error deleting message for everyone:', err);
    }
  }

  async getEditHistory(messageId: string): Promise<any[]> {
    try {
      const { data } = await this.supabase
        .from('messages')
        .select('edit_history')
        .eq('id', messageId)
        .single();

      return data?.edit_history || [];
    } catch (err) {
      console.error('Error fetching edit history:', err);
      return [];
    }
  }

  cleanup() {
    this.messageChannel?.unsubscribe();
    this.presenceChannel?.unsubscribe();
  }
}
