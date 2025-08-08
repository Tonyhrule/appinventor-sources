import { pipeline, env } from './transformers.js';
import './mememo.js';

const root = location.href.split('/').slice(0, -1).join('/');

env.allowLocalModels = true;
env.localModelPath = root + '/embedder';
env.allowRemoteModels = false;

const embedderPromise = pipeline(
  'feature-extraction',
  'Xenova/nomic-embed-text-v1',
  { device: 'webgpu', dtype: 'fp16' },
);

const hnsw = new window.mememo.HNSW({ distanceFunction: 'cosine' });

const embed = async (texts) => {
  const embedder = await embedderPromise;
  const embeddings = await embedder(texts, {
    pooling: 'mean',
    normalize: true,
  }).then((t) => t.tolist());
  return embeddings;
};

const queryHNSW = async (text, k = 5) => {
  if (hnsw.graphLayers.length === 0) {
    return {
      keys: [],
      distances: [],
    };
  }

  const [embedding] = await embed([text]);
  return hnsw.query(embedding, k);
};

window.fetchDocuments = async (prompt, topK = 5) => {
  const rawDocs = await queryHNSW(prompt, topK);
  const docs = rawDocs.keys.map((key, i) => ({
    title: key.split('\n')[0],
    content: key.split('\n').slice(1).join('\n'),
    distance: rawDocs.distances[i],
  }));

  Java.FetchedDocuments(JSON.stringify(docs));

  return `PROMPT:
${prompt}

DOCUMENTS:
${docs.map((doc) => `${doc.title}:\n${doc.content}`).join('\n\n')}`;
};

window.handleImport = () => {
  const dataString = Java.GetDatabase();
  if (!dataString) return;

  const data = JSON.parse(dataString);
  const { index, keys, embeddings } = data;

  hnsw.loadIndex(index);
  hnsw.bulkInsertSkipIndex(keys, embeddings);
};

handleImport();
