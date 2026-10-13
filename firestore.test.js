const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'demo-no-project';
const DB_ID = 'ai-studio-android-vhoqxeth-72b1dc1c-3df7-45db-85d8-6272f418425e';

test('Firestore Security Rules with Contacts and Shared Threads', async (t) => {
  let testEnv;

  try {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: '127.0.0.1',
        port: 8085,
        rules: fs.readFileSync('firestore.rules', 'utf8'),
      },
    });
  } catch (err) {
    console.error('Failed to initialize test environment:', err);
    throw err;
  }

  t.after(async () => {
    if (testEnv) await testEnv.cleanup();
  });

  await t.test('allow unauthenticated read of connection_probes', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const probeDoc = unauthedDb.collection('connection_probes').doc('test-probe');
    await assertSucceeds(probeDoc.get());
  });

  await t.test('allow valid connection probe write', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const probeDoc = unauthedDb.collection('connection_probes').doc('health-check');
    await assertSucceeds(
      probeDoc.set({
        status: 'ONLINE',
        databaseId: DB_ID,
        clientApp: 'Vhoqxeth Android',
      })
    );
  });

  await t.test('allow write and read of user contacts roster', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const contactDoc = unauthedDb.collection('users').doc('alice').collection('contacts').doc('bob');
    await assertSucceeds(
      contactDoc.set({
        contactUserId: 'bob',
        displayName: 'Bob Smith',
        currentIp: '192.168.1.50',
        addedAt: '2026-10-13T12:00:00Z',
      })
    );
    await assertSucceeds(contactDoc.get());
  });

  await t.test('reject contact write with missing required fields', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const contactDoc = unauthedDb.collection('users').doc('alice').collection('contacts').doc('bad_bob');
    await assertFails(
      contactDoc.set({
        addedAt: '2026-10-13T12:00:00Z',
      })
    );
  });

  await t.test('allow write and read of two-way shared threads', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const threadDoc = unauthedDb.collection('threads').doc('thread_alice_bob');
    await assertSucceeds(
      threadDoc.set({
        threadId: 'thread_alice_bob',
        participantIds: ['alice', 'bob'],
        lastMessage: 'Hello from Alice!',
        lastSenderId: 'alice',
        lastSenderName: 'Alice',
        updatedAt: '2026-10-13T12:00:00Z',
      })
    );
    await assertSucceeds(threadDoc.get());
  });

  await t.test('allow write and read of thread messages', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const msgDoc = unauthedDb.collection('threads').doc('thread_alice_bob').collection('messages').doc('msg_1');
    await assertSucceeds(
      msgDoc.set({
        messageId: 'msg_1',
        threadId: 'thread_alice_bob',
        senderId: 'alice',
        senderName: 'Alice',
        content: 'Encrypted payload',
        timestamp: '2026-10-13T12:00:00Z',
      })
    );
    await assertSucceeds(msgDoc.get());
  });

  await t.test('allow deletion of contact, thread, and messages', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const contactDoc = unauthedDb.collection('users').doc('alice').collection('contacts').doc('bob');
    const threadDoc = unauthedDb.collection('threads').doc('thread_alice_bob');
    const msgDoc = threadDoc.collection('messages').doc('msg_1');
    await assertSucceeds(msgDoc.delete());
    await assertSucceeds(threadDoc.delete());
    await assertSucceeds(contactDoc.delete());
  });
});
