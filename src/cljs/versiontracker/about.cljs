(ns versiontracker.about)

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/vt-logo.svg"}]
   [:div
    [:p "It looks like these days almost every IT company is using CI/CD and for sure they all do this for a good reason. But is it always clear what software versions are running in what environment? In (larger) companies with lots of in house developed software, maybe in combination with commercial software, it might be difficult to keep track of all the software versions deployed to their test, acceptance or even production environment. That's why I started creating this application; to visualize the architectural landscape of an IT-environment and integrate it with your CI/CD."]
    [:p "The idea is that within an environment (test, acceptance, production), applications (systems) can be added as nodes. These nodes can have sub-nodes (website on Apache server). Then both nodes and sub-nodes can be linked together through there interfaces as sources (sending) and targets (receiving)."]
    [:p "This application is maintained as Open Source and can be found" [:a {:href "https://github.com/ILoveHubGit/versiontracker/" :target "_blank"} " here "] "on Github."]]])
