# Quick Start: Documentation Guide

**Start here if you're new to the Synapse project!**

---

## 🚀 5-Minute Overview

### What is Synapse?
A fast, offline-first, open-source social media platform built with Angular and Supabase.

### Key Features
- 📱 Works offline with service workers
- 🔄 Real-time updates via WebSockets
- 📲 Installable as a native app (PWA)
- 🔒 Secure authentication with Supabase
- 💬 Direct messaging, posts, stories, notifications

### Tech Stack
- **Frontend**: Angular 21, TypeScript, Tailwind CSS
- **Backend**: Supabase (PostgreSQL + Auth + Realtime)
- **Storage**: Cloudinary / Cloudflare R2
- **Build**: Vite

---

## 📚 Documentation Files

### Start Here
1. **README.md** (5 min read)
   - Project overview
   - Quick start
   - Features list

2. **DEVELOPER_GUIDE.md** (30 min read)
   - Setup instructions
   - Architecture overview
   - Development workflow

3. **DOCUMENTATION_INDEX.md** (10 min read)
   - Navigation guide
   - Quick reference
   - File index

---

## ⚡ Quick Setup (10 minutes)

### Prerequisites
```bash
# Check you have Node.js 18+
node --version
npm --version
```

### Installation
```bash
# 1. Clone repository
git clone https://github.com/SynapseOSS/Web.git
cd Web

# 2. Install dependencies
npm install

# 3. Set up environment
cp .env.example .env.local
# Edit .env.local with your Supabase credentials

# 4. Start development server
npm run dev

# 5. Open browser
# Visit http://localhost:3000
```

### First Steps
- [ ] Create an account
- [ ] Create a post
- [ ] Follow another user
- [ ] Send a message
- [ ] Create a story

---

## 🔍 Finding Information

### "How do I...?"

#### Set up the project?
→ **DEVELOPER_GUIDE.md** → Getting Started section

#### Create a new feature?
→ **DEVELOPER_GUIDE.md** → Development Workflow section

#### Understand the database?
→ **DEVELOPER_GUIDE.md** → Database Schema section

#### Deploy the app?
→ **DEVELOPER_GUIDE.md** → Deployment section

#### Fix a bug?
→ **DEVELOPER_GUIDE.md** → Troubleshooting section

#### Understand a service?
→ Search for the service in `src/services/` and read the JSDoc comments

#### Understand a component?
→ Search for the component in `src/components/` and read the JSDoc comments

---

## 📖 Documentation Structure

```
README.md
├── Project overview
├── Features
├── Tech stack
├── Quick start
└── Contributing

DEVELOPER_GUIDE.md
├── Architecture
├── Getting started
├── Project structure
├── Services (with examples)
├── Components
├── Authentication
├── Database schema
├── Development workflow
├── Testing
├── Deployment
└── Troubleshooting

DOCUMENTATION_INDEX.md
├── Quick reference
├── File index
├── Service index
├── Component index
└── External resources

Source Code JSDoc
├── File-level documentation
├── Class documentation
├── Method documentation
├── Parameter documentation
└── Usage examples
```

---

## 🎯 Common Tasks

### Task 1: Create a Post
1. Navigate to `/app/compose`
2. Enter post text
3. Add media (optional)
4. Click "Post"
5. Post appears in feed

**Code Location**: `src/pages/compose.component.ts`  
**Service**: `src/services/post.service.ts`

### Task 2: Add a New Service
1. Create file: `src/services/my-feature.service.ts`
2. Add JSDoc comments
3. Implement methods
4. Inject in components
5. Add tests

**Example**: `src/services/post.service.ts`  
**Guide**: DEVELOPER_GUIDE.md → Development Workflow

### Task 3: Create a New Page
1. Create component: `src/pages/my-page.component.ts`
2. Add route in `src/app.routes.ts`
3. Add navigation link
4. Implement functionality
5. Add tests

**Example**: `src/pages/feed.component.ts`  
**Guide**: DEVELOPER_GUIDE.md → Development Workflow

### Task 4: Deploy to Production
1. Build: `npm run build`
2. Choose platform (Vercel, Netlify, Firebase)
3. Set environment variables
4. Deploy
5. Verify

**Guide**: DEVELOPER_GUIDE.md → Deployment

---

## 🔐 Key Concepts

### Authentication
- Users sign up with email/password
- Supabase handles authentication
- JWT tokens stored in localStorage
- Auth guard protects routes

**Learn More**: DEVELOPER_GUIDE.md → Authentication & Security

### Real-time Updates
- WebSocket connections via Supabase Realtime
- Automatic subscription to data changes
- Live feed, messages, notifications

**Learn More**: `src/services/realtime.service.ts`

### Offline Support
- Service worker caches data
- App works without internet
- Syncs when connection restored

**Learn More**: `src/services/pwa.service.ts`

### Row-Level Security
- Database-level access control
- Policies enforce permissions
- Users can only access their own data

**Learn More**: DEVELOPER_GUIDE.md → Database Schema

---

## 🛠️ Development Tools

### Commands
```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm test             # Run tests
npm run test:watch   # Run tests in watch mode
npm run test:coverage # Generate coverage report
```

### Browser DevTools
- **Console**: Check for errors
- **Network**: Monitor API calls
- **Application**: Inspect service worker
- **Performance**: Profile performance

### Angular DevTools
- Install Angular DevTools extension
- Inspect component tree
- Monitor change detection
- Profile performance

---

## 📚 Learning Resources

### Official Documentation
- [Angular](https://angular.dev)
- [Supabase](https://supabase.com/docs)
- [TypeScript](https://www.typescriptlang.org/docs/)
- [Tailwind CSS](https://tailwindcss.com/docs)

### Project Documentation
- README.md - Project overview
- DEVELOPER_GUIDE.md - Complete guide
- DOCUMENTATION_INDEX.md - Navigation
- JSDoc comments - Code documentation

### External Resources
- [PWA Documentation](https://web.dev/progressive-web-apps/)
- [RxJS Guide](https://rxjs.dev)
- [Vite Guide](https://vitejs.dev)

---

## ❓ FAQ

### Q: How do I get started?
A: Read README.md, then DEVELOPER_GUIDE.md, then follow the setup instructions.

### Q: Where is the database schema?
A: DEVELOPER_GUIDE.md → Database Schema section

### Q: How do I create a new feature?
A: DEVELOPER_GUIDE.md → Development Workflow section

### Q: How do I deploy?
A: DEVELOPER_GUIDE.md → Deployment section

### Q: Where are the services?
A: `src/services/` directory. Read JSDoc comments for documentation.

### Q: Where are the components?
A: `src/components/` and `src/pages/` directories. Read JSDoc comments.

### Q: How do I run tests?
A: `npm test` or `npm run test:watch`

### Q: How do I debug?
A: Use browser DevTools (F12) and check console for errors.

### Q: What if I get an error?
A: Check DEVELOPER_GUIDE.md → Troubleshooting section

### Q: How do I contribute?
A: Read README.md → Contributing section

---

## 🚦 Next Steps

### For New Developers
1. ✅ Read this file (5 min)
2. ✅ Read README.md (5 min)
3. ✅ Read DEVELOPER_GUIDE.md (30 min)
4. ✅ Set up development environment (10 min)
5. ✅ Explore source code (30 min)
6. ✅ Create your first feature (1-2 hours)

### For Experienced Developers
1. ✅ Skim README.md (2 min)
2. ✅ Review DEVELOPER_GUIDE.md (10 min)
3. ✅ Check DOCUMENTATION_INDEX.md (5 min)
4. ✅ Review relevant source files (10 min)
5. ✅ Start developing (immediately)

### For DevOps/Deployment
1. ✅ Read DEVELOPER_GUIDE.md → Deployment (10 min)
2. ✅ Choose deployment platform
3. ✅ Set environment variables
4. ✅ Deploy application
5. ✅ Monitor and maintain

---

## 📞 Getting Help

### Documentation
- Check README.md
- Check DEVELOPER_GUIDE.md
- Check DOCUMENTATION_INDEX.md
- Read JSDoc comments in source files

### Code Issues
- Check browser console (F12)
- Check network requests
- Review error messages
- Check DEVELOPER_GUIDE.md → Troubleshooting

### Questions
- Open GitHub issue
- Check GitHub discussions
- Email support@synapse.social

---

## 📋 Checklist: First Day

- [ ] Read README.md
- [ ] Read DEVELOPER_GUIDE.md
- [ ] Set up development environment
- [ ] Run `npm run dev`
- [ ] Create an account
- [ ] Create a post
- [ ] Explore the codebase
- [ ] Read JSDoc comments
- [ ] Run tests: `npm test`
- [ ] Create a simple feature

---

## 🎓 Learning Path

### Week 1: Foundations
- Day 1: Read documentation
- Day 2: Set up environment
- Day 3: Explore codebase
- Day 4: Create simple feature
- Day 5: Review and refactor

### Week 2: Development
- Day 1: Create service
- Day 2: Create component
- Day 3: Add tests
- Day 4: Deploy to staging
- Day 5: Code review

### Week 3+: Mastery
- Contribute features
- Review pull requests
- Mentor new developers
- Optimize performance
- Improve documentation

---

## 🏆 Best Practices

### Code
- Use TypeScript types
- Add JSDoc comments
- Follow code style
- Write tests
- Review before committing

### Documentation
- Update JSDoc when changing code
- Update guides for new features
- Keep examples current
- Review during code reviews
- Maintain version history

### Development
- Create feature branches
- Use conventional commits
- Write meaningful messages
- Test before pushing
- Request code review

---

## 📞 Support

**Need help?**
- 📖 Check documentation files
- 💬 Open GitHub issue
- 📧 Email support@synapse.social
- 🌐 Visit synapse.social

---

**Ready to start? Begin with README.md!**

---

**Last Updated**: November 26, 2024  
**Version**: 1.0.0
