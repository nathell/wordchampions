(ns kobrem.mui
  (:require [reagent.core :refer [adapt-react-class as-element]]))

(def app-bar (adapt-react-class js/MUI.AppBar))
(def menu (adapt-react-class js/MUI.Menu))
(def menu-item (adapt-react-class js/MUI.MenuItem))
(def paper (adapt-react-class js/MUI.Paper))
(def theme-provider (adapt-react-class js/MUI.MuiThemeProvider))
