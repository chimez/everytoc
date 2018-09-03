(ns app.everytoc.sidebar
  (:require [app.everytoc.common]
            [reagent.core :as r]
            ["antd/lib/tree" :as Tree]
            ["antd/lib/layout" :as Layout]
            ["antd/lib/row" :as Row]
            ["antd/lib/col" :as Col]
            [clojure.string :as string]
            [clojure.walk :as walk]))

(def Sider (.-Sider Layout))
(def Content (.-Content Layout))
(def TreeNode (.-TreeNode Tree))

(defonce toc (r/atom [:div]))
(defonce toc-map (r/atom {}))

;; make toc component

;; (3, [0 5 2]) => [0 5 3]
;; (2, [0 5 2]) => [0 6]
;; (2, [0 5]) => [0 6]
;; (2, [0]) => [0 0]
(defn make-key-array [num previous]
  (let [length (count previous)
        this (cond (> length num) (let [this (pop previous)]
                                    (make-key-array num this))
                   (== length num) (let [n (peek previous)
                                         this (pop previous)
                                         this (conj this (+ n 1))]
                                     this)
                   (< length num) (let [this (conj previous 0)]
                                    (if (== (+ 1 length) num)
                                      this
                                      (make-key-array num this))))]
    this))
;; ((h1 h2)) => [[0] [0 0]...]
(defn build-key-array [l]
  (let [ previous (atom [])
        stack (atom [])]
    (doseq [ni l]
      (let [this (make-key-array ni @previous)]
        (reset! previous this)
        (swap! stack conj this)
        ))
    @stack))

;; ((h1 h2),[t1 t2]) => {0 {},1 {}...}
(defn make-toc-map [tags texts]
  (let [a (atom {})]
    (doseq [[x y] (map list (build-key-array tags) texts)]
      (swap! a assoc-in x {:start [:> TreeNode {:title y :key (string/join "-" x)}]}))
    ;; (println @a)
    @a))

;; ({map}) => [vec]
(defn go [m]
  (let [ks (keys m)
        stack (atom [])]
    (doseq [k ks]
      (if (= k :start)
        (do
          ;; (println (get m k))
          (let [st (atom (get m k))]
            (doseq [i @stack]
              (swap! st conj i))
            (reset! stack @st))
          ;; (doseq [i (get m k)]
            ;; (swap! stack conj i))
          )
        (swap! stack conj (go (get m k)))))
    ;; (println @stack)
    @stack))

(defn jump-to-url [selectedKeys]
  (let [k (get (js->clj selectedKeys) 0)
        id (get @toc-map k)]
    (js/browser.runtime.sendMessage (clj->js {:source "sidebar" :id id}))))

;; ([h1 h2 h3],[t1 t2 t3]) => [:> Tree ...]
(defn make-toc [tags texts]
  (let [v (atom [:> Tree {:defaultExpandAll true :onSelect (fn [selectedKeys, e] (jump-to-url selectedKeys))}])
        v1 (go (make-toc-map (for [x tags] x) texts))]
    (doseq [x v1]
      (swap! v conj x))
    ;; (println @v)
    @v))


;; receive page message and flush toc

(defn receive-from-tab [message]
  (let [tags (get message "tagName_array")
        tags (map (fn [s] (js/Number (subs s 1))) tags)
        texts (get message "innerText_array")
        ids (get message "id_array")]
    ;; (println "message from tab" tags texts)

    (reset! toc (make-toc tags texts))

    (let [ks  (for [x (build-key-array tags)] (string/join "-" x))
          vs ids]
      (reset! toc-map (zipmap ks vs)))
    ))

(defn handle-message [message]
  (let [message (js->clj message)
        source (get message "source")]
    ;; (println message)
    (cond (= source "tab") (receive-from-tab message))))

(js/browser.runtime.onMessage.addListener handle-message)


;; generate sidebar
(defn sidebar []
  [:> Row
   [:> Col
    @toc]])

(defn ^:export run []
  (r/render [sidebar]
            (js/document.getElementById "app")))

(run)
