import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService - Bug Fix Verification', () => {
  let service: ThemeService;
  let displaySetSpy: jasmine.Spy;

  beforeEach(() => {
    localStorage.clear();
    
    // Spy on document.body.style.display setter BEFORE service creation
    displaySetSpy = jasmine.createSpy('displaySetter');
    Object.defineProperty(document.body.style, 'display', {
      set: displaySetSpy,
      get: () => '',
      configurable: true
    });
    
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    // Restore original property
    Object.defineProperty(document.body.style, 'display', {
      value: '',
      writable: true,
      configurable: true
    });
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should toggle dark mode state', () => {
    const initialMode = service.darkMode();
    service.toggle();
    expect(service.darkMode()).toBe(!initialMode);
  });

  /**
   * CRITICAL BUG FIX TEST:
   * This test verifies that toggle() does NOT hide the document body.
   * 
   * BEFORE FIX: toggle() would set document.body.style.display = 'none'
   * causing accessibility issues and visual flashing.
   * 
   * AFTER FIX: toggle() should never manipulate body display property.
   */
  it('should NOT hide document body during toggle (BUG FIX)', (done) => {
    displaySetSpy.calls.reset();
    
    service.toggle();
    
    // Wait for any async operations
    setTimeout(() => {
      // Verify body.style.display was never set to 'none'
      const callsToNone = displaySetSpy.calls.all().filter(call => call.args[0] === 'none');
      expect(callsToNone.length).toBe(0, 'document.body.style.display should never be set to "none"');
      done();
    }, 100);
  });

  it('should prevent rapid successive toggles', (done) => {
    const initialMode = service.darkMode();
    
    // Attempt multiple rapid toggles
    service.toggle();
    service.toggle();
    service.toggle();
    
    setTimeout(() => {
      // Should only toggle once due to isTransitioning guard
      expect(service.darkMode()).toBe(!initialMode);
      done();
    }, 100);
  });

  it('should persist theme to localStorage', (done) => {
    service.darkMode.set(true);
    
    setTimeout(() => {
      expect(localStorage.getItem('theme')).toBe('dark');
      
      service.darkMode.set(false);
      setTimeout(() => {
        expect(localStorage.getItem('theme')).toBe('light');
        done();
      }, 0);
    }, 0);
  });

  it('should apply dark class to document element when dark mode is enabled', (done) => {
    service.darkMode.set(true);
    
    setTimeout(() => {
      expect(document.documentElement.classList.contains('dark')).toBe(true);
      expect(document.documentElement.style.colorScheme).toBe('dark');
      done();
    }, 0);
  });

  it('should remove dark class when light mode is active', (done) => {
    service.darkMode.set(true);
    
    setTimeout(() => {
      service.darkMode.set(false);
      
      setTimeout(() => {
        expect(document.documentElement.classList.contains('dark')).toBe(false);
        expect(document.documentElement.style.colorScheme).toBe('light');
        done();
      }, 0);
    }, 0);
  });

  it('should update meta theme-color tag based on theme', (done) => {
    const themeColorMeta = document.createElement('meta');
    themeColorMeta.name = 'theme-color';
    document.head.appendChild(themeColorMeta);
    
    service.darkMode.set(true);
    
    setTimeout(() => {
      const meta = document.querySelector('meta[name="theme-color"]') as HTMLMetaElement;
      expect(meta?.content).toBe('#020617');
      
      service.darkMode.set(false);
      setTimeout(() => {
        expect(meta?.content).toBe('#ffffff');
        document.head.removeChild(themeColorMeta);
        done();
      }, 0);
    }, 0);
  });
});
