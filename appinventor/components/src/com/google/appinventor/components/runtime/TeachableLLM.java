// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2023 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

/**
 * TeachableLLM Component, based on the Teachable Machine extension
 * 
 * @author richard134x@gmail.com (Richard A. Xiong)
 *
 */

package com.google.appinventor.components.runtime;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.FileScope;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.FileStreamReadOperation;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.SdkLevel;
import org.json.JSONObject;
import android.app.Activity;
import android.view.WindowManager.LayoutParams;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The TeachableLLM component is a non-visible component for chatting with a local AI chatbot. This
 * version uses WebLLM to run the ChatBot locally. It also has support for RAG using MeMemo and a
 * supplementary website.
 *
 * @author richard134x@gmail.com (Richard A. Xiong)
 */
@DesignerComponent(version = YaVersion.TEACHABLELLM_COMPONENT_VERSION,
    description = "The TeachableLLM component is a non-visible component for chatting with a local AI chatbot. This "
        + "has support for RAG using MeMemo and a supplementary website.",
    category = ComponentCategory.EXPERIMENTAL, nonVisible = true,
    iconName = "images/teachablellm.png")
@UsesAssets(fileNames = "TeachableLLM.html, TeachableLLM.js")
@SimpleObject
public final class TeachableLLM extends AndroidNonvisibleComponent {
  private final Logger LOG = Logger.getLogger(TeachableLLM.class.getName());
  private WebView webView = null;
  private ChatBot chatBot = null;
  private String database = "";

  private void registerFiles() {
    form.fileCache.registerFile("transformers.js",
        "https://cdn.jsdelivr.net/npm/@huggingface/transformers");
    form.fileCache.registerFile("mememo.js",
        "https://cdn.jsdelivr.net/npm/mememo/dist/index.umd.js");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/tokenizer_config.json",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/tokenizer_config.json");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/onnx/model_fp16.onnx",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/onnx/model_fp16.onnx");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/tokenizer.json",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/tokenizer.json");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/config.json",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/config.json");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/vocab.txt",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/vocab.txt");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/quantize_config.json",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/quantize_config.json");
    form.fileCache.registerFile("embedder/Xenova/nomic-embed-text-v1/special_tokens_map.json",
        "https://huggingface.co/Xenova/nomic-embed-text-v1/resolve/main/special_tokens_map.json");
  }

  /**
   * Creates a new component.
   *
   * @param container container, component will be placed in
   */
  public TeachableLLM(ComponentContainer container) {
    super(container.$form());
    registerFiles();
    WebView.setWebContentsDebuggingEnabled(true);
    requestHardwareAcceleration(form);

    webView = new WebView(container.$form());

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        final String url = request.getUrl().toString();
        try {
          if (!url.startsWith("http://localhost/")) {
            return null;
          }
          final InputStream file;
          final String path = url.replace("http://localhost/", "");
          if (path.startsWith("TeachableLLM")) {
            file = form.openAsset(path);
          } else {
            file = new FileInputStream(form.fileCache.getFile(path).get());
          }
          if (file == null) {
            return null;
          }
          String charSet;
          String contentType;

          if (url.endsWith(".json")) {
            contentType = "application/json";
            charSet = "UTF-8";
          } else if (url.endsWith("html")) {
            contentType = "text/html";
            charSet = "UTF-8";
          } else if (url.endsWith(".js")) {
            contentType = "application/javascript";
            charSet = "UTF-8";
          } else if (url.endsWith(".wasm")) {
            contentType = "application/wasm";
            charSet = "binary";
          } else if (url.endsWith(".css")) {
            contentType = "text/css";
            charSet = "UTF-8";
          } else if (url.endsWith(".png")) {
            contentType = "image/png";
            charSet = "binary";
          } else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            contentType = "image/jpeg";
            charSet = "binary";
          } else {
            contentType = "application/octet-stream";
            charSet = "binary";
          }

          if (SdkLevel.getLevel() >= SdkLevel.LEVEL_LOLLIPOP) {
            HashMap<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Access-Control-Allow-Origin", "*");
            return new WebResourceResponse(contentType, charSet, 200, "OK", responseHeaders, file);
          } else {
            return new WebResourceResponse(contentType, charSet, file);
          }
        } catch (Exception e) {
          LOG.severe("Error handling web resource request: " + e.getMessage());
        }

        return null;
      }
    });

    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setAllowFileAccess(true);
    webView.addJavascriptInterface(new JsObject(), "Java");
    webView.loadUrl("http://localhost/TeachableLLM.html");
  }

  private static void requestHardwareAcceleration(Activity activity) {
    activity.getWindow().setFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED,
        LayoutParams.FLAG_HARDWARE_ACCELERATED);
  }

  /**
   * Specifies the path of the database file.
   *
   * @internaldoc
   *              <p/>
   *              See {@link MediaUtil#determineMediaSource} for information about what a path can
   *              be.
   *
   * @param path the path of the database file
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET)
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, userVisible = false)
  public void DatabaseFile(String path) {
    if (path != null && !path.isEmpty()) {
      new FileStreamReadOperation(form, this, "ReadFrom", "//" + path, FileScope.App, false) {
        @Override
        public boolean process(String json) {
          database = json;
          webView.evaluateJavascript("handleImport()", null);
          return true;
        }

        @Override
        public void onError(IOException e) {
          LOG.severe("Error reading database file: " + e.getMessage());
        }
      }.run();
    }
  }

  /**
   * Sets the chat bot component to use.
   *
   * @param chatBot the chat bot component to use
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT)
  @SimpleProperty(category = PropertyCategory.GENERAL)
  public void ChatBotComponent(ChatBot chatBot) {
    this.chatBot = chatBot;
  }

  /**
   * Sends a message to the TeachableLLM and fetches relevant context for the message. This includes
   * all previous messages in the chat. A topK of 5 means the top 5 results will be returned. The
   * context prompt is used to determine how retrieved context will be used.
   *
   * @param prompt the prompt to send to the TeachableLLM
   * @param stream whether to stream the response
   * @param topK the number of top K results to fetch (Eg. 5)
   * @param contextPrompt the context prompt to send to the TeachableLLM
   */
  @SimpleFunction(
      description = "Sends a message to the TeachableLLM and fetches relevant context for the message. This "
          + "includes all previous messages in the chat. A topK of 5 means the top 5 results will be "
          + "returned. The context prompt is used to determine how retrieved context will be used.")
  public void ConverseWithContext(String message, int topK, String contextPrompt) {
    if (message != null && !message.isEmpty()) {
      webView.evaluateJavascript("fetchDocuments(" + JSONObject.quote(message) + ", " + topK
          + ").then((documents) => Java.HandleDocuments(documents, "
          + JSONObject.quote(contextPrompt) + "))", null);
    }
  }

  /**
   * Fires when the Teachable LLM fetches documents to use.
   */
  @SimpleEvent(description = "Fires when the Teachable LLM fetches documents to use.")
  public void FetchedDocuments(String documents) {
    form.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          EventDispatcher.dispatchEvent(TeachableLLM.this, "FetchedDocuments",
              JsonUtil.getObjectFromJson(documents, true));
        } catch (Exception e) {
          LOG.severe("Error dispatching FetchedDocuments event: " + e.getMessage());
        }
      }
    });
  }

  private class JsObject {
    @JavascriptInterface
    public void FetchedDocuments(String documents) {
      TeachableLLM.this.FetchedDocuments(documents);
    }

    @JavascriptInterface
    public void HandleDocuments(String documents, String contextPrompt) {
      if (chatBot != null) {
        chatBot.Converse(contextPrompt + "\n\n" + documents);
      }
    }

    @JavascriptInterface
    public String GetDatabase() {
      return database;
    }
  }
}
