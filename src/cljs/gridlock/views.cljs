(ns gridlock.views
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf :refer [dispatch]]
    [reagent.core :as r]))

(def <sub (comp deref rf/subscribe))

(defn msg [id]
  (<sub [:i18n-message id]))

(defn merge-props
  ([m] m)
  ([m1 m2] (r/merge-props m1 m2))
  ([m1 m2 & ms] (r/merge-props m1 (apply merge-props m2 ms))))

(defn diagram-corner []
  [:div.diagram-corner])

(defn tile [source c movable?]
  (if movable?
    (r/with-let [hide? (r/atom nil)]
      [:div.tile
       (merge-props
        {:draggable true
         :on-drag-start #(do (-> % (aget "dataTransfer") (.setData "text/plain" c))
                             (dispatch [:start-drag source])
                             (reset! hide? true))
         :on-drag-end #(do (dispatch [:end-drag])
                           (reset! hide? false))
         :on-touch-start #(dispatch [:tap-tile source])
         :class (when @hide? "tile-hidden")}
        (when (= source (<sub [:current-tile]))
          {:class "tile-current"}))
       c])
    [:div.tile c]))

(defn- border-placing [x y]
  (cond (= x 0) [:left   (dec y)]
        (= x 4) [:right  (dec y)]
        (= y 0) [:top    (dec x)]
        (= y 4) [:bottom (dec x)]))

(defn diagram
  [{:keys [diagram-number placed finished highlighted demo small] :as desc}]
  (prn "demo" demo "small" small "cd" (<sub [:current-diagram]) "cond" (or (and demo (not small))
                                                                           #_(= diagram-number (<sub [:current-diagram]))))
  [:div.diagram (merge-props (when finished {:class "diagram-finished"})
                             (when highlighted {:class "diagram-highlighted"})
                             (if (or (and demo (not small))
                                     (and (not demo) (= diagram-number (<sub [:current-diagram]))))
                               {:class "diagram-current"}
                               {:class "diagram-small"})
                             {:on-click #(dispatch [:set-current-diagram diagram-number])})
   (r/with-let [highlighted-fields (r/atom #{})]
     (doall
      (for [y (range 5) x (range 5)
            :let [corner? (#{[0 0] [0 4] [4 0] [4 4]} [x y])
                  [border border-pos] (border-placing x y)
                  letter-pos (+ y y y x -4)
                  in-area? (not (or corner? border))
                  letter (when in-area? (nth placed letter-pos))
                  placable? (= letter ".")
                  droppable? (and placable? (<sub [:can-place? diagram-number]))]]
        (if (and (= x 4) (= y 0) (not finished) (not demo))
          ^{:key (str x "-" y)}
          [:div.reload
           [:svg {:on-click #(dispatch [:clean-diagram diagram-number])}
            [:use {:xlink-href "#rotate-ccw"}]]]
          ^{:key (str x "-" y)}
          [:div.diagram-item
           (cond->
               {:class (cond corner? "diagram-item-corner"
                             border  "diagram-item-border")}
             (and in-area? (@highlighted-fields letter-pos)) (r/merge-props {:class "diagram-item-highlighted"})
             droppable? (assoc :on-drag-enter #(swap! highlighted-fields conj letter-pos)
                               :on-drag-leave #(swap! highlighted-fields disj letter-pos)
                               :on-drag-over #(do (.preventDefault %) (.stopPropagation %))
                               :on-drop #(do (.preventDefault %) (swap! highlighted-fields empty) (dispatch [:drop-diagram {:diagram-number diagram-number, :letter-pos letter-pos}]))
                               :on-touch-start #(dispatch [:tap-diagram {:diagram-number diagram-number, :letter-pos letter-pos}])))
           (cond corner? nil
                 border  (get-in desc [border border-pos])
                 :else   (when (and letter (not placable?))
                           [tile {:diagram-number diagram-number, :letter-pos letter-pos} letter (not finished)]))]))))])

(defn nine [{:keys [nine-number letters]}]
  (into
   [:div.nine]
   (for [[i letter] (map-indexed vector letters) :when (not= letter \.)]
     [tile {:nine-number nine-number, :letter-pos i} letter true])))

(defn nines-area []
  (r/with-let [highlighted (r/atom 0)] ; integer, not boolean,
    [:div.nines-area
     {:on-drag-enter #(swap! highlighted inc)
      :on-drag-leave #(swap! highlighted dec)
      :on-drag-over #(do (.preventDefault %) (.stopPropagation %))
      :on-drop #(do (.preventDefault %) (reset! highlighted 0) (dispatch [:drop-nines]))
      :class (when (pos? @highlighted) "nines-area-highlighted")}
     (for [word (<sub [:nines])]
       ^{:key (:nine-number word)}
       [nine word])]))

(defn diagrams-area []
  [:div.diagrams-area
   (for [item (<sub [:diagrams])]
     ^{:key (:diagram-number item)}
     [diagram item])])

(defn game []
  [:div.panel.game
   [diagrams-area]
   [nines-area]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:hint])} (msg :hint)]
    [:button.button {:on-click #(dispatch [:reset])} (msg :reset)]
    [:button.button {:on-click #(dispatch [:restart])} (msg :new-game)]]])

(defn selected-start []
  [:div.panel.game
   [:div.diagrams-area
    (for [i (range (<sub [:requested-difficulty]))]
      ^{:key i} [diagram {:demo true, :small (pos? i)}])]
   [:div.nines-area
    (for [i (range (<sub [:requested-difficulty]))]
      ^{:key i} [nine {}])]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:start])} (msg :start-game)]
    [:button.button.how-to-play-button
     {:on-click #(dispatch [:show-how-to-play])}
     (msg :how-to-play)]]])

(defn language-picker []
  (let [current-language (<sub [:language])
        languages [:pl :nl :en]]
    (into
     [:div.language-picker]
     (for [language languages]
       [:img {:src (str "img/" (name language) ".svg")
              :class (when (= language current-language) "selected")
              :on-click #(dispatch [:set-language language])}]))))

(defn how-to-play-pl []
  [:div.how-to-play
   [:h1 "Cel gry"]
   [:p "Na ekranie pojawi się " [:i "N"] " kwadratowych diagramów o wymiarach 3×3, z dwunastoma literami na brzegach każdego diagramu, i " [:i "N"] " dziewięcioliterowych słów."]
   [diagram {:top "apr",
             :bottom "sya",
             :left "skp",
             :right "żza",
             :placed ".........",
             :highlighted false,
             :finished false,
             :demo true}]
   [:div.nines-area
    [nine {:letters "lustracja"}]]
   [:p "Twoim celem jest dopasowanie diagramów do słów, tak że litery z każdego słowa wypełniają szczelnie jeden z diagramów, tworząc sześć poprawnych wyrazów 5-literowych (trzy w poziomie i trzy w pionie)."]
   [diagram {:top "apr",
             :bottom "sya",
             :left "skp",
             :right "żza",
             :placed "tralucasj",
             :highlighted false,
             :finished false,
             :demo true}]
   [:p "Liczba " [:i "N"] " zależy od poziomu trudności: 1 – łatwy, 3 – średni, 5 – trudny."]
   [:p [:a {:target "_blank", :rel "noopener", :href "https://www.youtube.com/watch?v=7ec6j31nlAk"} "Zobacz wideo"] " pokazujące zasady gry."]
   [:h1 "Jak grać"]
   [:p "W przeglądarce na komputerze: przeciągaj litery ze słów w dolnej części ekranu na diagramy. Kiedy umieścisz literę na diagramie, gra zapamięta to i następne litery na tym diagramie będą mogły pochodzić tylko z tego samego słowa."]
   [:p "Jeśli popełnisz błąd, możesz przeciągnąć literę z powrotem do dolnej części ekranu; powróci ona na wyjściowe miejsce. Możesz też zresetować diagram dotykając ikony " [:svg [:use {:xlink-href "#rotate-ccw"}]] " w prawym górnym rogu, albo wszystkie diagramy klikając " [:i "Resetuj"] "."]
   [:p "W przeglądarce mobilnej: jeden z diagramów jest pokazywany w pełnym rozmiarze, a pozostałe jako miniatury. Ten w pełnym rozmiarze jest aktywny. Dotknij diagramu, żeby go aktywować. Aby umieścić literę, dotknij jej, a potem dotknij miejsca docelowego na aktywnym diagramie."]
   [:p "Gdy poprawnie wypełnisz cały diagram, jest on zaznaczany specjalnym kolorem i wyłączany z dalszej gry."]
   [:p "Możesz poprosić o podpowiedź klikając " [:i "Podpowiedź"] ". Jeśli wszystkie dotychczas umieszczone litery są na właściwych pozycjach, to gra wybierze losowo jedną z pozostałych liter i przeniesie ją na właściwe miejsce.
Jeżeli jednak któraś litera jest zagrana niepoprawnie, zostanie ona zdjęta i umieszczona z powrotem w dziewięcioliterowym słowie."]
   [:button.button.start-button
    {:on-click #(dispatch [:go-back])}
    "Wróć"]])

(defn how-to-play-nl []
  [:div.how-to-play
   [:h1 "Doel van het spel"]
   [:p "Het spel begint met raster van " [:i "N"] " 3×3 lege vakjes, met 12 letters rondom aan de zijkant, en " [:i "N"] " negen-letter woorden."]
   [diagram {:top "ead",
             :bottom "lsn",
             :left "aek",
             :right "rsl",
             :placed ".........",
             :highlighted false,
             :finished false,
             :demo true}]
   [:div.nines-area
    [nine {:letters "geleerden"}]]
   [:p "Het doel van het spel is om de letters van het negen-letter woord zo in het diagram te plaatsen, op zo'n manier dat er 6 geldige Nederlandse woorden worden gevormd (drie horizontaal en drie verticaal)."]
   [diagram {:top "ead",
             :bottom "lsn",
             :left "aek",
             :right "rsl",
             :placed "ndegelere",
             :highlighted false,
             :finished false,
             :demo true}]
   [:p [:i "N"] " is afhankelijk van de moeilijkheidsgraad: 1 voor makkelijk, 3 voor gemiddeld, 5 voor moeilijk."]
   [:p [:a {:target "_blank", :rel "noopener", :href "https://www.youtube.com/watch?v=7ec6j31nlAk"} "Bekijk de video"] " (in het Pools) van de TV show waarop dit spel is gebaseerd."]
   [:h1 "Speluitleg"]
   [:p "Op de desktop, sleep de letters vanonderaf in het raster. Als je een letter hebt geplaatst in het raster, dan onthoud het spel uit welk negenletterwoord deze afkomstig was, en vanaf dat moment kan je in dat betreffende raster alleen nog letters uit datzelfde woord plaatsen."]
   [:p "Als je een fout maakt, sleep de letter terug naar het negeletterwoorden gedeelte; de letter gaan dan terug naar zijn originele plek. Je kan ook het diagram resetten door rechtsboven het diagram op het " [:svg [:use {:xlink-href "#rotate-ccw"}]] " icoontje te klikken, of reset alle diagrammen door op de " [:i "Reset"] " knop te klikken."]
   [:p "Op mobiel krijg je één diagram op volledige grootte te zien, en de overige in miniatuur. Tik op een miniatuur om te wisselen tussen de diagrammen. Om een letter te plaatsen, tik op de letter en tik daarna op een positie in het huidige diagram."]
   [:p "Als een diagram correct is opgelost dan wordt het gemarkeerd met een kleur en het betreffende diagram is dan uitgesloten van het spel."]
   [:p "Als je vast zit, klik op " [:i "Hint"] ". Als alle letter tot dusver correct geplaatst zijn, dan wordt een willekeurige ongebruikte letter op de juiste plek in het juiste diagram gezet. Als je een fout hebt gemaakt, dan al één van de verkeerd geplaatste letters terug worden gezet. (vertaling: Bas de Reuver)"]
   [:button.button.start-button
    {:on-click #(dispatch [:go-back])}
    "Back"]])
	
(defn how-to-play-en []
  [:div.how-to-play
   [:h1 "Objective"]
   [:p "You are given " [:i "N"] " 3×3 square diagrams, with 12 letters shown on the sides of each diagram, and " [:i "N"] " nine-letter words."]
   [diagram {:top "spc",
             :bottom "dak",
             :left "ile",
             :right "yry",
             :placed ".........",
             :highlighted false,
             :finished false,
             :demo true}]
   [:div.nines-area
    [nine {:letters "translate"}]]
   [:p "Your task is to match words against the diagrams so that letters from every nine-letter word fill one of the diagrams to form six valid English words (three horizontal and three vertical)."]
   [diagram {:top "spc",
             :bottom "dak",
             :left "ile",
             :right "yry",
             :placed "talasentr",
             :highlighted false,
             :finished false,
             :demo true}]
   [:p [:i "N"] " depends on the difficulty level: 1 for Easy, 3 for Normal, 5 for Hard."]
   [:p [:a {:target "_blank", :rel "noopener", :href "https://www.youtube.com/watch?v=7ec6j31nlAk"} "See a video"] " (in Polish) of a TV show featuring the game."]
   [:h1 "How to play"]
   [:p "On desktop, drag letters from the words in bottom area onto the diagrams. Once you place a letter on a diagram, the game will remember which word you took that letter from, and only allow subsequent letters from the same word on that diagram."]
   [:p "If you make a mistake, drop the letter back onto the nine-letter word area; it will return to its original position. You can also reset the whole diagram by clicking the " [:svg [:use {:xlink-href "#rotate-ccw"}]] " icon in the top-right corner, or all diagrams by clicking " [:i "Reset"] "."]
   [:p "On mobile, you are shown one diagram in full size, and the rest in miniature. The full-size diagram is the current one. To switch between diagrams, tap a miniature. To place a letter, tap it, and then tap the target position on the current diagram."]
   [:p "When you solve one of the diagrams correctly, it is marked with a colour and removed from further play."]
   [:p "If you’re stuck, press " [:i "Hint"] ". If all letters you have placed so far are in correct positions, an unused letter will be picked at random and placed on the correct diagram. If you have made an error, one of the wrongly placed letters will be returned to its word."]
   [:button.button.start-button
    {:on-click #(dispatch [:go-back])}
    "Back"]])

(defn how-to-play []
  (case (<sub [:language])
    :en [how-to-play-en]
    :pl [how-to-play-pl]
    :nl [how-to-play-nl]
    nil))

(defn welcome []
  [:div.welcome
   [language-picker]
   [:div.title (msg :hello)]
   [:div.buttons
    [:button.button.start-button
     {:on-click #(dispatch [:select-difficulty])}
     (msg :start-game)]
    [:button.button.how-to-play-button
     {:on-click #(dispatch [:show-how-to-play])}
     (msg :how-to-play)]
    [:a.button
     {:href (str "mailto:dj@danieljanus.pl?subject="
                 (string/replace (msg :word-champions) " " "%20"))}
     (msg :feedback)]]])

(defn success []
  [:div.panel.success
   [diagrams-area]
   [:h1 (msg :hooray)]
   [:div.buttons
    [:button.button
     {:on-click #(dispatch [:share])}
     (msg :share)]
    [:button.button
     {:on-click #(dispatch [:restart])}
     (msg :play-again)]]])

(defn difficulty []
  [:div.panel.difficulty
   (when (= (<sub [:language]) :pl)
     [:div
      [:h2 "Wybierz słownik:"]
      [:div.dictionaries
       [:div.dictionary
        {:class (when (<sub [:dictionary-selected? :nkjp]) "selected")
         :on-click #(dispatch [:set-dictionary :nkjp])}
        [:h3 "Częste słowa"]
        [:p "(w tym imiona i inne nazwy własne)"]
        [:p "279 zagadek"]]
       [:div.dictionary
        {:class (when (<sub [:dictionary-selected? :osps]) "selected")
         :on-click #(dispatch [:set-dictionary :osps])}
        [:h3 "Oficjalny Słownik Polskiego Scrabblisty"]
        [:p "(trudne słowa, bez nazw własnych)"]
        [:p "2 000 zagadek"]
        [:p [:small "Udostępniony dzięki uprzejmości "
             [:a {:href "http://pfs.org.pl"
                  :target "_blank"
                  :rel "noopener"} "Polskiej Federacji Scrabble"]]]]]])
   [:h2 (msg :select-difficulty)]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:start 1])} (msg :easy)]
    [:button.button {:on-click #(dispatch [:start 3])} (msg :normal)]
    [:button.button {:on-click #(dispatch [:start 5])} (msg :hard)]]])

(defn root []
  [:div.root
   [:div.title-bar (<sub [:title-bar])]
   (if-not (<sub [:loaded?])
     [:div.main-panel (msg :loading-dictionaries) "..."]
     [:div.main-panel
      (condp = (<sub [:mode])
        :before-start [welcome]
        :before-start-selected [selected-start]
        :how-to-play [how-to-play]
        :difficulty [difficulty]
        :success [success]
        [game])])
   [:div.author "© "
    [:a {:target "_blank", :rel "noopener", :href "http://danieljanus.pl"} "Daniel Janus"]
    " 2017–2020"
    [:span.is-hidden-mobile
     " | "
     (msg :written-in-cljs)
     " | "
     [:a {:target "_blank", :rel "noopener", :href "https://github.com/nathell/gridlock"} (msg :source-code)]]]
   [:div.toast
    {:class (when (<sub [:toast-shown?]) "shown")}
    (msg (<sub [:toast-message]))]])
