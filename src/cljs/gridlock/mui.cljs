(ns gridlock.mui
  (:require [reagent.core :refer [adapt-react-class as-element]]
            cljsjs.material-ui
            cljsjs.material-ui-svg-icons))

(def app-bar (adapt-react-class js/MaterialUI.AppBar))
(def card (adapt-react-class js/MaterialUI.Card))
(def card-actions (adapt-react-class js/MaterialUI.CardActions))
(def card-header (adapt-react-class js/MaterialUI.CardHeader))
(def card-text (adapt-react-class js/MaterialUI.CardText))
(def raised-button (adapt-react-class js/MaterialUI.RaisedButton))
(def menu (adapt-react-class js/MaterialUI.Menu))
(def menu-item (adapt-react-class js/MaterialUI.MenuItem))
(def paper (adapt-react-class js/MaterialUI.Paper))
(def theme-provider (adapt-react-class js/MaterialUI.MuiThemeProvider))
