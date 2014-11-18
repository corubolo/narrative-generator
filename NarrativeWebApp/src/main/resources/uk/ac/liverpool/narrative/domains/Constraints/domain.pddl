(define (domain romance)

(:types place locatable - object
        thing character - locatable
        animal person - character )

(:constants
 low medium high - level
 morning afternoon evening night - timeofday
 )

(:predicates 
			 (alive ?x - character)
             (knows ?x - character ?y - object)
             (has ?x - character ?y - thing)
			 (at ?loc - place ?char - character)
			 (connected ?loca ?locb - place)

			 (good ?c - character)
			 (sociable ?c - character)
			 (hunger ?x - character ?l - level)
			 (fear ?x - character ?y - character ?l - level)
             (isclosed ?x - place)
			 (affinity ?x ?y - character ?l - level)
 		 	 (happy ?x - character ?l - level)
			 (time ?t - timeofday)

			 )

(:action goto
:parameters (?x - character ?y - place ?z - place)
:precondition (and (at ?y ?x) (connected ?y ?z) (knows ?x ?z) (alive ?x) (not(isclosed ?z)))
:effect (and (not (at ?y ?x)) (at ?z ?x))
)

(:action give
  :parameters (?giver ?taker - person ?t - thing ?p - place)
  :precondition (and (has ?giver ?t) (alive ?giver) (alive ?taker)  (at ?p ?taker) (at ?p ?giver) (not (= ?taker ?giver)) (knows ?giver ?taker))
  :effect (and (has ?taker ?t) (not (has ?giver ?t)))
)

(:action greet
  :parameters (?chara ?charb - character ?p - place)
  :precondition (and (sociable ?chara) (alive ?chara) (alive ?charb) (at ?p ?chara) (at ?p ?charb) (not (= ?chara ?charb)) )
  :effect (and (knows ?chara ?charb) (knows ?charb ?chara) )
)

(:action open
  :parameters (?x - character ?p - place)
  :precondition (and (isclosed ?p) (at ?p ?x))
  :effect (and (not (isclosed ?p)) )
)

(:action close
  :parameters (?x - character ?p - place)
  :precondition (and (not (isclosed ?p)) (at ?p ?x))
  :effect (and (isclosed ?p) )
)

)
