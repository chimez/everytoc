(ns app.everytoc.background
  (:require [app.everytoc.common]))

;; send js to tab
(def get-all-h
  "var nodeIterator = document.createNodeIterator(
    document.body,
    NodeFilter.SHOW_ELEMENT,
    function(node) {
        return (node.nodeName.toLowerCase() === 'h1'
                ||node.nodeName.toLowerCase() === 'h2'
                ||node.nodeName.toLowerCase() === 'h3'
                ||node.nodeName.toLowerCase() === 'h4'
                ||node.nodeName.toLowerCase() === 'h5'
                ||node.nodeName.toLowerCase() === 'h6')
               ? NodeFilter.FILTER_ACCEPT
               : NodeFilter.FILTER_REJECT;});
  var currentNode;
  var innerText_array=[];
  var tagName_array=[];
  var id_array=[];
  while (currentNode = nodeIterator.nextNode()) {
        innerText_array.push(currentNode.innerText);
        tagName_array.push(currentNode.tagName);
        id_array.push(currentNode.id)};")
(def send-back "
  browser.runtime.sendMessage({
    source: \"tab\",
    tagName_array:tagName_array,
    innerText_array:innerText_array,
    id_array:id_array,});")

(defn exec-js-tab [tab-id]
  (js/browser.tabs.executeScript #js {:code get-all-h})
  (js/browser.tabs.executeScript #js {:code send-back}))

(js/browser.tabs.onActivated.addListener exec-js-tab)
(js/browser.tabs.onUpdated.addListener exec-js-tab)
;; (js/window.addEventListener "load" (fn [x] (println "!!!!!!!!!!!!") (exec-js-tab x)))
;; jump in the page
(defn message-from-sidebar [message]
  (let [id (get message "id")]
    (when-not (= id "")
      (let [code  (str "document.location=`${location.origin}${location.pathname}#" id "`;")]
        ;; (println code)
      (js/browser.tabs.executeScript #js {:code code})))))
(defn receive-runtime [message]
  (let [message (js->clj message)
        source (get message "source")]
    ;; (println message)
    (cond (= source "sidebar") (message-from-sidebar message))))
(js/browser.runtime.onMessage.addListener receive-runtime)
