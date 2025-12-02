import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiKeysService, ApiKey, ApiKeyWithSecret } from '../services/api-keys.service';

@Component({
  selector: 'app-settings-api-keys',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-4xl mx-auto p-6">
      <h1 class="text-2xl font-bold mb-2">API Keys</h1>
      <p class="text-gray-600 dark:text-gray-400 mb-6">
        Generate API keys to access Synapse programmatically
      </p>

      @if (newKey()) {
        <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-6">
          <h3 class="font-semibold mb-2 flex items-center gap-2">
            <span class="text-yellow-600">⚠️</span> Save your API key
          </h3>
          <p class="text-sm text-gray-700 dark:text-gray-300 mb-3">
            This key will only be shown once. Copy it now and store it securely.
          </p>
          <div class="bg-white dark:bg-gray-800 rounded p-3 font-mono text-sm break-all border">
            {{ newKey()!.key }}
          </div>
          <button
            (click)="copyKey(newKey()!.key)"
            class="mt-3 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
          >
            {{ copied() ? '✓ Copied!' : 'Copy to Clipboard' }}
          </button>
          <button
            (click)="newKey.set(null)"
            class="mt-3 ml-2 px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded hover:bg-gray-300 dark:hover:bg-gray-600 text-sm"
          >
            Done
          </button>
        </div>
      }

      <div class="mb-6">
        <button
          (click)="showGenerateModal.set(true)"
          class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          [disabled]="loading()"
        >
          + Generate New Key
        </button>
      </div>

      @if (loading()) {
        <div class="text-center py-8">Loading...</div>
      } @else if (keys().length === 0) {
        <div class="text-center py-12 text-gray-500">
          No API keys yet. Generate one to get started.
        </div>
      } @else {
        <div class="space-y-3">
          @for (key of keys(); track key.id) {
            <div class="border dark:border-gray-700 rounded-lg p-4 flex items-center justify-between">
              <div class="flex-1">
                <div class="font-mono text-sm">{{ key.key_prefix }}</div>
                <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  {{ key.name }}
                </div>
                <div class="text-xs text-gray-500 mt-1">
                  Created {{ formatDate(key.created_at) }}
                  @if (key.last_used_at) {
                    · Last used {{ formatDate(key.last_used_at) }}
                  }
                </div>
              </div>
              <div class="flex items-center gap-2">
                @if (key.status === 'active') {
                  <span class="px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 text-xs rounded">
                    Active
                  </span>
                } @else {
                  <span class="px-2 py-1 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 text-xs rounded">
                    Revoked
                  </span>
                }
                @if (key.status === 'active') {
                  <button
                    (click)="revokeKey(key.id)"
                    class="px-3 py-1 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded text-sm"
                  >
                    Revoke
                  </button>
                }
              </div>
            </div>
          }
        </div>
      }

      @if (showGenerateModal()) {
        <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" (click)="showGenerateModal.set(false)">
          <div class="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full mx-4" (click)="$event.stopPropagation()">
            <h2 class="text-xl font-bold mb-4">Generate API Key</h2>
            <div class="mb-4">
              <label class="block text-sm font-medium mb-2">Key Name (Optional)</label>
              <input
                type="text"
                [(ngModel)]="keyName"
                placeholder="e.g., Production API"
                class="w-full px-3 py-2 border dark:border-gray-600 rounded dark:bg-gray-700"
              />
            </div>
            <div class="flex gap-2">
              <button
                (click)="generateKey()"
                class="flex-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                [disabled]="loading()"
              >
                Generate
              </button>
              <button
                (click)="showGenerateModal.set(false)"
                class="px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded hover:bg-gray-300 dark:hover:bg-gray-600"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class SettingsApiKeysComponent implements OnInit {
  private apiKeysService = inject(ApiKeysService);

  keys = signal<ApiKey[]>([]);
  newKey = signal<ApiKeyWithSecret | null>(null);
  loading = signal(false);
  showGenerateModal = signal(false);
  copied = signal(false);
  keyName = '';

  async ngOnInit() {
    await this.loadKeys();
  }

  async loadKeys() {
    this.loading.set(true);
    try {
      const keys = await this.apiKeysService.listKeys();
      this.keys.set(keys);
    } catch (error) {
      console.error('Failed to load API keys:', error);
    } finally {
      this.loading.set(false);
    }
  }

  async generateKey() {
    this.loading.set(true);
    try {
      const key = await this.apiKeysService.generateKey(this.keyName || undefined);
      this.newKey.set(key);
      this.showGenerateModal.set(false);
      this.keyName = '';
      await this.loadKeys();
    } catch (error) {
      console.error('Failed to generate API key:', error);
      alert('Failed to generate API key');
    } finally {
      this.loading.set(false);
    }
  }

  async revokeKey(keyId: string) {
    if (!confirm('Are you sure you want to revoke this API key? This action cannot be undone.')) {
      return;
    }
    try {
      await this.apiKeysService.revokeKey(keyId);
      await this.loadKeys();
    } catch (error) {
      console.error('Failed to revoke API key:', error);
      alert('Failed to revoke API key');
    }
  }

  async copyKey(key: string) {
    await navigator.clipboard.writeText(key);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2000);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
