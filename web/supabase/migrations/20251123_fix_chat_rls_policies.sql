-- Drop existing policies
DROP POLICY IF EXISTS "Users can create chats" ON public.chats;
DROP POLICY IF EXISTS "Users can join chats" ON public.chat_participants;
DROP POLICY IF EXISTS "Chat creators can update chats" ON public.chats;

-- Recreate policies with proper permissions
CREATE POLICY "Users can create chats"
  ON public.chats FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Users can join chats as participants"
  ON public.chat_participants FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Chat participants can update chats"
  ON public.chats FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM public.chat_participants
      WHERE chat_participants.chat_id = chats.chat_id
      AND chat_participants.user_id = auth.uid()
    )
  );
