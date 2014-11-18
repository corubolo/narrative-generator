(define (domain Jasmine)
(:requirements :typing)
(:types place locatable - object
        thing character - locatable
        genie - character
)
(:predicates (at ?x - locatable ?y - place) 
             (in ?x - thing ?y - thing)
             (has ?x - character ?y - thing)
             (single ?x - character)
             (loves ?x - character ?y - character)
	     (married-to ?x - character ?y - character)
	     (female ?x - character)
	     (male ?x - character)
	     (dead ?x - character)
	     (alive ?x - character)

)

(:action travel
:parameters (?x - character ?y - place ?z - place)
:precondition (at ?x ?y)
:effect (and (not (at ?x ?y)) (at ?x ?z))
)




(:action give
:parameters (?x - character ?y - character ?z - thing ?t - place)
:precondition (and (has ?x ?z) (at ?x ?t) (at ?y ?t))
:effect (and (not (has ?x ?z)) (has ?y ?z))
)



(:action marry
:parameters (?x - character ?y - character ?p - place)
:precondition (and (at ?x ?p) (at ?y ?p) (female ?y) (male ?x) (single ?x) (single ?y) (alive ?x) (alive ?y) (loves ?x ?y) (loves ?y ?x))
:effect (and (married-to ?x ?y) (married-to ?y ?x) (not (single ?x)) (not (single ?y)))
)




(:action kill
:parameters (?x - character ?y - character ?p - place)
:precondition (and (at ?x ?p) (at ?y ?p) (alive ?x) (alive ?y))
:effect (and (not (alive ?y)) (dead ?y)) 
)



(:action love-spell
:parameters (?x - genie ?y - character ?z - character ?p - place)
:precondition (and (at ?x ?p) (at ?y ?p) (alive ?x) (alive ?y) (alive ?z))
:effect (loves ?z ?y)
)


)



