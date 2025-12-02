import { Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#6366f1"/>
          <stop offset="50%" stop-color="#8b5cf6"/>
          <stop offset="100%" stop-color="#d946ef"/>
        </linearGradient>
        <linearGradient id="logoGradient2" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#06b6d4"/>
          <stop offset="100%" stop-color="#6366f1"/>
        </linearGradient>
      </defs>
      <!-- Neural network nodes -->
      <circle cx="32" cy="12" r="6" fill="url(#logoGradient)"/>
      <circle cx="12" cy="32" r="6" fill="url(#logoGradient)"/>
      <circle cx="52" cy="32" r="6" fill="url(#logoGradient)"/>
      <circle cx="20" cy="52" r="6" fill="url(#logoGradient)"/>
      <circle cx="44" cy="52" r="6" fill="url(#logoGradient)"/>
      <!-- Center node -->
      <circle cx="32" cy="32" r="8" fill="url(#logoGradient2)"/>
      <!-- Connections -->
      <path d="M32 18 L32 24" stroke="url(#logoGradient)" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M18 32 L24 32" stroke="url(#logoGradient)" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M40 32 L46 32" stroke="url(#logoGradient)" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M24 46 L28 38" stroke="url(#logoGradient)" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M40 46 L36 38" stroke="url(#logoGradient)" stroke-width="2.5" stroke-linecap="round"/>
      <!-- Outer ring pulse -->
      <circle cx="32" cy="32" r="28" stroke="url(#logoGradient)" stroke-width="1.5" fill="none" opacity="0.3"/>
    </svg>
  `
})
export class LogoComponent {
  size = input<number>(40);
}
