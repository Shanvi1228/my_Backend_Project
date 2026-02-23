import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { getDocument, updateTitle } from '../api/documents.api';
import { useDocumentWebSocket } from '../hooks/useDocumentWebSocket';
import { useDocumentStore } from '../store/useDocumentStore';
import PresenceBar from '../components/PresenceBar';
import ChatSidebar from '../components/ChatSidebar';
import type { OperationMessage } from '../types/document.types';

const EditorPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [titleValue, setTitleValue] = useState('');
  const [showChat, setShowChat] = useState(true);
  const isApplyingRemote = useRef(false);
  const lastContentRef = useRef('');
  const connectedUsers = useDocumentStore((s) => s.connectedUsers);

  const { data: document, isLoading, error } = useQuery({
    queryKey: ['document', id],
    queryFn: () => getDocument(id!),
    enabled: !!id,
  });

  // Initialize TipTap editor
  const editor = useEditor({
    extensions: [StarterKit],
    content: document?.contentSnapshot ?? '',
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-xl mx-auto focus:outline-none min-h-[500px] p-8',
      },
    },
    onUpdate: ({ editor }) => {
      if (isApplyingRemote.current) return;
      
      const currentContent = editor.getHTML();
      if (currentContent === lastContentRef.current) return;
      
      // Detect changes and send operation
      // For simplicity, we're sending the full content
      // A production implementation would use proper OT operations
      const op: OperationMessage = {
        type: 'OPERATION',
        opType: 'INSERT',
        position: 0,
        content: currentContent,
        clientRevision: document?.currentRevision ?? 0,
      };
      
      sendOperation(op);
      lastContentRef.current = currentContent;
    },
  });

  // Handle remote operations from WebSocket
  const handleRemoteOperation = useCallback((op: OperationMessage) => {
    if (!editor || !op.content) return;
    
    isApplyingRemote.current = true;
    
    try {
      // Apply remote change to editor
      editor.commands.setContent(op.content);
      lastContentRef.current = op.content;
    } finally {
      isApplyingRemote.current = false;
    }
  }, [editor]);

  // WebSocket connection
  const { isConnected, sendOperation } = useDocumentWebSocket(id!, handleRemoteOperation);

  // Update editor content when document loads
  useEffect(() => {
    if (editor && document?.contentSnapshot) {
      editor.commands.setContent(document.contentSnapshot);
      lastContentRef.current = document.contentSnapshot;
      setTitleValue(document.title);
    }
  }, [editor, document]);

  const handleTitleEdit = () => {
    setIsEditingTitle(true);
  };

  const handleTitleSave = async () => {
    if (!id || !titleValue.trim() || titleValue === document?.title) {
      setIsEditingTitle(false);
      return;
    }

    try {
      await updateTitle(id, titleValue);
      setIsEditingTitle(false);
    } catch (error) {
      console.error('Failed to update title:', error);
      setTitleValue(document?.title ?? '');
      setIsEditingTitle(false);
    }
  };

  const handleTitleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleTitleSave();
    } else if (e.key === 'Escape') {
      setTitleValue(document?.title ?? '');
      setIsEditingTitle(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-gray-500">Loading document...</div>
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="flex flex-col items-center justify-center h-screen">
        <div className="text-red-500 mb-4">Failed to load document</div>
        <button
          onClick={() => navigate('/documents')}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Back to Documents
        </button>
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100vh-4rem)] bg-white">
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Connection status bar */}
        <div className={`px-4 py-2 text-sm ${isConnected ? 'bg-green-50 text-green-700' : 'bg-yellow-50 text-yellow-700'}`}>
          {isConnected ? '● Connected' : '○ Connecting...'}
        </div>

        {/* Presence bar */}
        <PresenceBar connectedUsers={connectedUsers} />

        {/* Title section */}
        <div className="border-b border-gray-200 px-8 py-4">
          {isEditingTitle ? (
            <input
              type="text"
              value={titleValue}
              onChange={(e) => setTitleValue(e.target.value)}
              onBlur={handleTitleSave}
              onKeyDown={handleTitleKeyDown}
              className="text-3xl font-bold text-gray-900 w-full focus:outline-none border-b-2 border-blue-500"
              autoFocus
            />
          ) : (
            <h1
              onClick={handleTitleEdit}
              className="text-3xl font-bold text-gray-900 cursor-pointer hover:text-gray-700"
              title="Click to edit"
            >
              {document.title}
            </h1>
          )}
          <div className="text-sm text-gray-500 mt-2">
            Owner: {document.ownerUsername} · Last updated: {new Date(document.updatedAt).toLocaleString()}
          </div>
        </div>

        {/* Editor */}
        <div className="flex-1 overflow-y-auto">
          <EditorContent editor={editor} />
        </div>

        {/* Toolbar */}
        <div className="border-t border-gray-200 px-4 py-2 flex items-center space-x-2">
          <button
            onClick={() => editor?.chain().focus().toggleBold().run()}
            className={`px-3 py-1 rounded ${editor?.isActive('bold') ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
            title="Bold"
          >
            <strong>B</strong>
          </button>
          <button
            onClick={() => editor?.chain().focus().toggleItalic().run()}
            className={`px-3 py-1 rounded ${editor?.isActive('italic') ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
            title="Italic"
          >
            <em>I</em>
          </button>
          <button
            onClick={() => editor?.chain().focus().toggleHeading({ level: 1 }).run()}
            className={`px-3 py-1 rounded ${editor?.isActive('heading', { level: 1 }) ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
            title="Heading 1"
          >
            H1
          </button>
          <button
            onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}
            className={`px-3 py-1 rounded ${editor?.isActive('heading', { level: 2 }) ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
            title="Heading 2"
          >
            H2
          </button>
          <button
            onClick={() => editor?.chain().focus().toggleBulletList().run()}
            className={`px-3 py-1 rounded ${editor?.isActive('bulletList') ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
            title="Bullet List"
          >
            •
          </button>
          <div className="flex-1"></div>
          <button
            onClick={() => setShowChat(!showChat)}
            className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            {showChat ? 'Hide Chat' : 'Show Chat'}
          </button>
        </div>
      </div>

      {/* Chat sidebar */}
      {showChat && <ChatSidebar documentId={id!} />}
    </div>
  );
};

export default EditorPage;