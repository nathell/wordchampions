(ns kobrem.mui
  (:require [reagent.core :refer [adapt-react-class as-element]]))

(def app-bar (adapt-react-class js/MUI.AppBar))
(def card (adapt-react-class js/MUI.Card))
(def card-actions (adapt-react-class js/MUI.CardActions))
(def card-header (adapt-react-class js/MUI.CardHeader))
(def card-text (adapt-react-class js/MUI.CardText))
(def raised-button (adapt-react-class js/MUI.RaisedButton))
(def menu (adapt-react-class js/MUI.Menu))
(def menu-item (adapt-react-class js/MUI.MenuItem))
(def paper (adapt-react-class js/MUI.Paper))
(def theme-provider (adapt-react-class js/MUI.MuiThemeProvider))
