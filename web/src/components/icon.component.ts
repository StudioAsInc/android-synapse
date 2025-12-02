import { Component, input, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [CommonModule],
  styles: [`
    img.light-icon {
      filter: brightness(0) saturate(100%);
    }
    img.dark-icon {
      filter: brightness(0) saturate(100%) invert(1);
    }
  `],
  template: `
    <img 
      [src]="iconUrl()"
      [alt]="name() + ' icon'"
      [width]="size()"
      [height]="size()"
      [class]="'inline-block ' + class() + ' ' + (isDark() ? 'dark-icon' : 'light-icon')"
    />
  `
})
export class IconComponent {
  name = input.required<string>();
  size = input<number | string>(24);
  class = input<string>('');
  filled = input<boolean>(false);
  
  isDark = signal(false);

  constructor() {
    // Check initial dark mode
    if (typeof document !== 'undefined') {
      this.isDark.set(document.documentElement.classList.contains('dark'));
      
      // Watch for dark mode changes
      const observer = new MutationObserver(() => {
        this.isDark.set(document.documentElement.classList.contains('dark'));
      });
      
      observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class']
      });
    }
  }

  private iconMap: Record<string, string> = {
    'menu': 'https://cdn-icons-png.flaticon.com/512/1828/1828859.png',
    'x': 'https://cdn-icons-png.flaticon.com/512/1828/1828778.png',
    'chevron-right': 'https://cdn-icons-png.flaticon.com/512/271/271228.png',
    'chevron-left': 'https://cdn-icons-png.flaticon.com/512/271/271220.png',
    'chevron-down': 'https://cdn-icons-png.flaticon.com/512/2985/2985150.png',
    'chevron-up': 'https://cdn-icons-png.flaticon.com/512/2985/2985161.png',
    'android': 'https://cdn-icons-png.flaticon.com/512/226/226770.png',
    'apple': 'https://cdn-icons-png.flaticon.com/512/731/731985.png',
    'windows': 'https://cdn-icons-png.flaticon.com/512/732/732221.png',
    'globe': 'https://cdn-icons-png.flaticon.com/512/814/814513.png',
    'monitor': 'https://cdn-icons-png.flaticon.com/512/2956/2956769.png',
    'terminal': 'https://cdn-icons-png.flaticon.com/512/2721/2721297.png',
    'zap': 'https://cdn-icons-png.flaticon.com/512/3093/3093155.png',
    'shield': 'https://cdn-icons-png.flaticon.com/512/3064/3064197.png',
    'users': 'https://cdn-icons-png.flaticon.com/512/1077/1077114.png',
    'download': 'https://cdn-icons-png.flaticon.com/512/724/724933.png',
    'rocket': 'https://cdn-icons-png.flaticon.com/512/3588/3588592.png',
    'calendar': 'https://cdn-icons-png.flaticon.com/512/2693/2693507.png',
    'tag': 'https://cdn-icons-png.flaticon.com/512/2541/2541988.png',
    'filter': 'https://cdn-icons-png.flaticon.com/512/5479/5479278.png',
    'check': 'https://cdn-icons-png.flaticon.com/512/5610/5610944.png',
    'bug': 'https://cdn-icons-png.flaticon.com/512/1087/1087927.png',
    'sparkles': 'https://cdn-icons-png.flaticon.com/512/2893/2893483.png',
    'github': 'https://cdn-icons-png.flaticon.com/512/733/733609.png',
    'google': 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png',
    'sun': 'https://cdn-icons-png.flaticon.com/512/2698/2698194.png',
    'moon': 'https://cdn-icons-png.flaticon.com/512/3688/3688129.png',
    'book-open': 'https://cdn-icons-png.flaticon.com/512/2232/2232688.png',
    'book': 'https://cdn-icons-png.flaticon.com/512/2232/2232688.png',
    'git-commit': 'https://cdn-icons-png.flaticon.com/512/2111/2111288.png',
    'credit-card': 'https://cdn-icons-png.flaticon.com/512/1170/1170678.png',
    'map': 'https://cdn-icons-png.flaticon.com/512/854/854929.png',
    'map-pin': 'https://cdn-icons-png.flaticon.com/512/684/684908.png',
    'help-circle': 'https://cdn-icons-png.flaticon.com/512/2354/2354573.png',
    'log-in': 'https://cdn-icons-png.flaticon.com/512/1828/1828490.png',
    'code': 'https://cdn-icons-png.flaticon.com/512/1005/1005141.png',
    'hash': 'https://cdn-icons-png.flaticon.com/512/3616/3616729.png',
    'twitter': 'https://cdn-icons-png.flaticon.com/512/733/733579.png',
    'linkedin': 'https://cdn-icons-png.flaticon.com/512/174/174857.png',
    'discord': 'https://cdn-icons-png.flaticon.com/512/5968/5968756.png',
    'youtube': 'https://cdn-icons-png.flaticon.com/512/1384/1384060.png',
    'mail': 'https://cdn-icons-png.flaticon.com/512/732/732200.png',
    'heart': 'https://cdn-icons-png.flaticon.com/512/2589/2589175.png',
    'message-circle': 'https://cdn-icons-png.flaticon.com/512/1380/1380338.png',
    'repeat': 'https://cdn-icons-png.flaticon.com/512/2618/2618245.png',
    'share': 'https://cdn-icons-png.flaticon.com/512/929/929468.png',
    'bookmark': 'https://cdn-icons-png.flaticon.com/512/5662/5662990.png',
    'more-horizontal': 'https://cdn-icons-png.flaticon.com/512/512/512142.png',
    'image': 'https://cdn-icons-png.flaticon.com/512/1829/1829586.png',
    'smile': 'https://cdn-icons-png.flaticon.com/512/742/742751.png',
    'bell': 'https://cdn-icons-png.flaticon.com/512/1827/1827349.png',
    'verified': 'https://cdn-icons-png.flaticon.com/512/7595/7595571.png',
    'send': 'https://cdn-icons-png.flaticon.com/512/3024/3024593.png',
    'search': 'https://cdn-icons-png.flaticon.com/512/622/622669.png',
    'plus': 'https://cdn-icons-png.flaticon.com/512/1828/1828817.png',
    'trash': 'https://cdn-icons-png.flaticon.com/512/3096/3096673.png',
    'image-plus': 'https://cdn-icons-png.flaticon.com/512/3342/3342137.png',
    'video': 'https://cdn-icons-png.flaticon.com/512/1179/1179069.png',
    'play': 'https://cdn-icons-png.flaticon.com/512/727/727245.png',
    'camera': 'https://cdn-icons-png.flaticon.com/512/685/685655.png',
    'bar-chart': 'https://cdn-icons-png.flaticon.com/512/2920/2920277.png',
    'lock': 'https://cdn-icons-png.flaticon.com/512/3064/3064155.png',
    'arrow-left': 'https://cdn-icons-png.flaticon.com/512/271/271220.png',
    'arrow-right': 'https://cdn-icons-png.flaticon.com/512/271/271228.png',
    'arrow-up': 'https://cdn-icons-png.flaticon.com/512/2985/2985161.png',
    'arrow-down': 'https://cdn-icons-png.flaticon.com/512/2985/2985150.png',
    'check-circle': 'https://cdn-icons-png.flaticon.com/512/5610/5610944.png',
    'alert-circle': 'https://cdn-icons-png.flaticon.com/512/564/564619.png',
    'info': 'https://cdn-icons-png.flaticon.com/512/2354/2354573.png',
    'file-text': 'https://cdn-icons-png.flaticon.com/512/3143/3143609.png',
    'trending-up': 'https://cdn-icons-png.flaticon.com/512/2985/2985088.png',
    'clock': 'https://cdn-icons-png.flaticon.com/512/2838/2838590.png',
    'archive': 'https://cdn-icons-png.flaticon.com/512/3143/3143609.png',
    'database': 'https://cdn-icons-png.flaticon.com/512/2906/2906274.png',
    'settings': 'https://cdn-icons-png.flaticon.com/512/2040/2040504.png',
    'mic': 'https://cdn-icons-png.flaticon.com/512/1082/1082810.png',
    'copy': 'https://cdn-icons-png.flaticon.com/512/1621/1621635.png',
    'reply': 'https://cdn-icons-png.flaticon.com/512/3024/3024593.png',
    'flag': 'https://cdn-icons-png.flaticon.com/512/3014/3014736.png',
    'edit': 'https://cdn-icons-png.flaticon.com/512/1159/1159633.png',
    'maximize': 'https://cdn-icons-png.flaticon.com/512/2089/2089649.png',
    'square': 'https://cdn-icons-png.flaticon.com/512/2874/2874802.png',
    'file': 'https://cdn-icons-png.flaticon.com/512/3143/3143609.png',
    'loader': 'https://cdn-icons-png.flaticon.com/512/2889/2889676.png'
  };

  iconUrl = computed(() => {
    return this.iconMap[this.name()] || 'https://cdn-icons-png.flaticon.com/512/471/471664.png';
  });
}
