---
layout: documentation
title: Extension
---

[&laquo; Back to index](index.html)
# Extension

Table of Contents:

* [TeachableLLM](#TeachableLLM)

## TeachableLLM  {#TeachableLLM}

The TeachableLLM component is a non-visible component for chatting with a local AI chatbot. This
 version uses WebLLM to run the ChatBot locally. It also has support for RAG using MeMemo and a
 supplementary website.



### Properties  {#TeachableLLM-Properties}

{:.properties}

{:id="TeachableLLM.ChatBotComponent" .component .wo} *ChatBotComponent*
: Sets the chat bot component to use.

{:id="TeachableLLM.DatabaseFile" .text .wo .do} *DatabaseFile*
: Specifies the path of the database file.

### Events  {#TeachableLLM-Events}

{:.events}

{:id="TeachableLLM.FetchedDocuments"} FetchedDocuments(*documents*{:.text})
: Fires when the Teachable LLM fetches documents to use.

### Methods  {#TeachableLLM-Methods}

{:.methods}

{:id="TeachableLLM.ConverseWithContext" class="method"} <i/> ConverseWithContext(*message*{:.text},*topK*{:.number},*contextPrompt*{:.text})
: Sends a message to the TeachableLLM and fetches relevant context for the message. This includes
 all previous messages in the chat. A topK of 5 means the top 5 results will be returned. The
 context prompt is used to determine how retrieved context will be used.
