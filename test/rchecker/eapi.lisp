(in-package :rchecker)

(defun tapply(request)
  #'(lambda()
      (list (apply (funcall *endpoint* (cmd-curl)) (funcall request))
            (apply (funcall *endpoint* (transport-drakma)) (funcall request)))))
;;; queue
(defun equeue-new (&key (router-id (get-event :router))
                     (description "description")
                     (predicate "1==1"))
  (tstep (format nil "Create new queue with predicate ~A."  predicate)
         (tapply (http-post (list "/routers" router-id "queues")
                            (jsown:new-js ("description" description)
                                          ("predicate" predicate))))
         (check-and (has-json) (has-key "ref") (publish-id :queue))))

(defun equeue-size (&key (router-id (get-event :router))
                      (id (get-event :queue))
                      (description (format nil "Get size of the queue."))
                      (checks (check-and (has-json) (has-key "size"))))
  (tstep description
         (tapply (http-get "/routers" router-id "queues" id "size"))
         checks ))

(defun equeue-put (&key (router-id (get-event :router))
                     (id (get-event :queue))
                     (description "description")
                     (predicate "1==1") )
  (tstep (format nil "Replace or create queue.")
         (tapply (http-put (list "/routers" router-id "queues" id) (jsown:new-js
                                                                     ("description" description)
                                                                     ("predicate" predicate))))
         (check-and (has-json) (has-key "ref") (publish-id :queue))))

(defun equeue-del (&key (router-id (get-event :router))
                     (id (get-event :queue)))
  (tstep (format nil "Delete queue ~A." id)
         (tapply (http-del  "/routers" router-id "queues" id))
         (check-and (is-equal "") (remove-id :queue))))

;;; agent
(defun eagent-new (&key (router-id (get-event :router))
                     (address "address")
                     (capabilities (jsown:new-js ("language" "en"))))
  (tstep (format nil "Create new agent with capabilities ~A."  (jsown:to-json capabilities))
         (tapply (http-post (list "/routers" router-id "agents") (jsown:new-js
                                                                   ("address" address)
                                                                   ("capabilities" capabilities))))
         (check-and (has-json) (has-key "ref") (publish-id :agent))))

(defun eagent-put (&key (router-id (get-event :router))
                     (id (get-event :agent))
                     (address "address")
                     (capabilities (jsown:new-js ("language" "en"))))
  (tstep (format nil "Replace or create agent.")
         (tapply (http-put (list "/routers" router-id "agents" id) (jsown:new-js
                                                              ("address" address)
                                                              ("capabilities" capabilities))))
         (check-and (has-json) (has-key "ref") (publish-id :agent))))

(defun eagent-set (&key (router-id (get-event :router))
                     (id (get-event :agent))
                     (address "address")
                     (state "ready") ;; offline busy
                     (capabilities (jsown:new-js ("language" "en"))))
  (tstep (format nil "Set properties on agent ~A." (jsown:to-json (jsown:new-js
                                                                    ("address" address)
                                                                    ("state" state)
                                                                    ("capabilities" capabilities))))
         (tapply (http-post (list "/routers" router-id "agents" id) (jsown:new-js
                                                               ("address" address)
                                                               ("state" state)
                                                               ("capabilities" capabilities))))
         (check-and (is-equal "") #'(lambda(res) (funcall (fire-event :agent-state) state)
                                           (list t (list(format nil "ok - publish agent-state -> ~A" state)))))))

(defun eagent-del(&key (router-id (get-event :router))
                    (id (get-event :agent)))
  (tstep (format nil "Delete agent ~A." id)
         (tapply (http-del  "/routers" router-id "agents" id))
         (check-and (is-equal "") (remove-id :agent))))

;;; router
(defun erouter-new (&key (name "name") (description "description"))
  (tstep (format nil "Create new router.")
         (tapply (http-post "/routers" (jsown:new-js("name" name)
                                                    ("description" description))))
         (check-and (has-json) (has-key "ref") (publish-id :router))))

(defun erouter-all (&key (checks (check-and (has-json))))
  (tstep (format nil "List available routers.")
         (tapply (http-get "/routers"))
         checks))

(defun erouter-put (&key (id (get-event :router)) (name "name") (description "description"))
  (tstep (format nil "Replace or create router.")
         (tapply (http-put (list "/routers" id) (jsown:new-js ("name" name)
                                                              ("description" description))))
         (check-and (has-json) (has-key "ref") (publish-id :router))))

(defun erouter-del (&key (id (get-event :router)))
  (tstep (format nil "Delete router ~A." id)
         (tapply (http-del "/routers" id))
         (check-and (is-equal "") (remove-id :router))))
;;; task
(defun etask-new(&key (router-id (get-event :router))
                   (tag :null)
                   (requirements (jsown:new-js ("key" t)))
                   (callback-url (format nil "http://localhost:4343/task?router=~A&sleep=~A" router-id (random 2)))
                   (context (jsown:new-js ("key" "value")))
                   (queue-id (get-event :queue))
                   (plan-id :null)
                   (checks (check-and (has-json) (has-key "ref") (publish-id :task))))
  (tstep (format nil "Create new task to queue ~A, plan ~A and context ~A." queue-id plan-id (jsown:to-json context))
         (tapply (http-post (list "/routers" router-id "tasks")
                            (jsown:new-js
                              ("tag" tag)
                              ("callbackUrl" callback-url)
                              ("userContext" context)
                              ("requirements" requirements)
                              ("queueRef" queue-id)
                              ("planRef" plan-id))))
         checks))

(defun etask-del (&key (router-id (get-event :router))
                    (id (get-event :task)))
  (tstep (format nil "Delete a task ~A." id)
         (tapply (http-del  "/routers" router-id "tasks" id))
         (check-and (is-equal "") (remove-id :task))))


(defun etask(&key (state "assigned")
               (description (format nil "Check that task is in state ~A." state))
               (router-id (get-event :router))
               (id (get-event :task))

               (checks (check-and (has-json) (has-kv "state" state))))
  (tstep description
         (tapply (http-get "/routers" router-id "tasks" id ))
         checks))

(defun etask-by-tag(&key (tag "unique-tag")
                      (description (format nil "Check that there are tasks with the specified ~A tag." tag))
                      (router-id (get-event :router))
                      (checks (check-and (has-json))))
  (tstep description
         (tapply (http-get "/routers" router-id "tasks" (format nil "byTag?tag=~A" tag)))
         checks))

(defun etask-set(&key (router-id (get-event :router)) (id (get-event :task)) (state :null) )
  (tstep (format nil "Set task's state = ~A." state )
         (tapply (http-post (list "/routers" router-id "tasks" id)
                            (jsown:new-js ("state" state) )))
         (is-equal "")))

(defun etask-all(&key (router-id (get-event :router)))
  (tstep "List all tasks"
         (tapply (http-get "/routers" router-id "tasks"))
         (not-contains "error")))

(defun etask-set-context(&key (router-id (get-event :router)) (task-id (get-event :task)) (key "key") (value "value"))
  (tstep (format nil "Set task's context~A = ~A." key value)
         (tapply (http-put (list "/routers" router-id "tasks" task-id "user_context" key)
                            value))
         (is-equal "")))

;;; agent
(defun eagent-all(&key (router-id (get-event :router)))
  (tstep "List all agents"
         (tapply (http-get "/routers" router-id "agents"))
         (not-contains "error")))

(defun eagent-set(&key
                    (state "ready") ;; offline busy
                    (description (format nil "Set state=~A of the agent"state ))
                    (router-id (get-event :router))
                    (id (get-event :agent))
                    (address "address")
                    (capabilities (jsown:new-js ("language" "en"))))
  (tstep description
         (tapply (http-post (list "/routers" router-id "agents" id)
                            (jsown:new-js
                              ("address" address)
                              ("state" state)
                              ("capabilities" capabilities))))
         (is-equal "")))

(defun eagent(&key (description "Get state of the agent") (router-id (get-event :router))
                (id (get-event :agent)) (checks (has-kv "ref" id)))
  (tstep description
         (tapply (http-get "/routers" router-id "agents" id))
         checks))

(defun eagent-del(&key (router-id (get-event :router))
                    (id (get-event :agent)))
  (tstep (format nil "Delete agent with id ~A." id)
         (tapply (http-del "/routers" router-id "agents" id))
         (is-equal "")))
;;;
(defun eplan-new(&key (router-id (get-event :router))
                   (queue-id (get-event :queue))
                   (default-queue-id (get-event :queue))
                   (predicate "1 ==1")
                   (priority 0)
                   (next-route nil)
                   (timeout 3600)
                   (default-timeout 3600)
                   (rules (list (jsown:new-js ("tag" "test-rule")
                                              ("predicate" predicate)
                                              ("routes" (append
                                                         (list (jsown:new-js
                                                                 ("queueRef" queue-id)
                                                                 ("priority" priority)
                                                                 ("timeout" timeout)))
                                                         next-route)))))
                   (description "description")
                   (default-route (jsown:new-js
                                    ("queueRef" default-queue-id)
                                    ("priority" 0)
                                    ("timeout" default-timeout)))
                   (checks (check-and (has-json) (has-key "ref"))))
  (tstep (format nil "Create new plan to queue ~A with predicate ~A." queue-id predicate)
         (tapply (http-post (list "/routers" router-id "plans")
                            (jsown:new-js
                              ("rules" rules)
                              ("description" description)
                              ("defaultRoute" default-route))))
         checks))
(defun eplan-put(&key (router-id (get-event :router))
                   (id (get-event :plan))
                   (queue-id (get-event :queue))
                   (default-queue-id (get-event :queue))
                   (predicate "1 ==1")
                   (priority 0)
                   (next-route nil)
                   (timeout 3600)
                   (default-timeout 3600)
                   (rules (list (jsown:new-js ("tag" "test-rule")
                                              ("predicate" predicate)
                                              ("routes" (append
                                                         (list (jsown:new-js
                                                                 ("queueRef" queue-id)
                                                                 ("priority" priority)
                                                                 ("timeout" timeout)))
                                                         next-route)))))
                   (description "description")
                   (default-route (jsown:new-js
                                    ("queueRef" default-queue-id)
                                    ("priority" 0)
                                    ("timeout" default-timeout)))
                   (checks (check-and (has-json) (has-key "ref"))))
  (tstep (format nil "Create new plan to queue ~A with predicate ~A." queue-id predicate)
         (tapply (http-put (list "/routers" router-id "plans" id)
                            (jsown:new-js
                              ("rules" rules)
                              ("description" description)
                              ("defaultRoute" default-route))))
         checks))

(defun eplan-del(&key (router-id (get-event :router))
                   (id (get-event :plan)))
  (tstep (format nil "Delete plan with id ~A." id)
         (tapply (http-del "/routers" router-id "plans" id))
         (is-equal "")))
