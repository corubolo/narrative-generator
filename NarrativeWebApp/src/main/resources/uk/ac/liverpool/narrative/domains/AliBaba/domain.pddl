(define (domain AliBaba)

(:types location locatable job secretWords - thing
		place - location
		securePlace - place
        object character - locatable
        animal person - character 
)

(:constants
 low medium high - level
 woodcutter - job
 wood - object
 )

(:predicates 
			 (alive ?x - character)
             (knows ?x - character ?y - thing)
             (has ?haver - thing ?possession - thing)
			 (at ?loc - location ?char - character)
			 (connected ?loca ?locb - location)

			 (good ?c - character)
			 (sociable ?c - character)
			 (hunger ?x - character ?l - level)
			 (fear ?x - character ?y - character ?l - level)
             (isclosed ?x - location)
			 (works-as ?c - character ?j - job)
 			 (likes ?a ?b - character)
			 (owns ?c - character  ?t - thing)
			 (busy ?p - securePlace)
			 (isOpening ?c - character ?p - location ?w - secretWords)
		     (isFollowing ?c1 ?c2 - character)
			 (married-to ?c1 ?c2 - character)
			 )

(:action goto
:parameters (?x - character ?y - place ?z - place)
:precondition (and (at ?y ?x) (connected ?y ?z) (knows ?x ?z) (alive ?x) )
:effect (and (not (at ?y ?x)) (at ?z ?x))
)

(:action enter
:parameters (?x - character ?y - place ?z - securePlace ?w - secretWords)
:precondition (and (at ?y ?x) (knows ?x ?z) (alive ?x) (not(isclosed ?z)) (not (busy ?z))  (isOpening ?x ?z ?w))
:effect (and (not (at ?y ?x)) (at ?z ?x) (busy ?z) (not (isOpening ?x ?z ?w)))
)

(:action exit
:parameters (?x - character ?y - securePlace  ?z - place)
:precondition (and (at ?y ?x) (knows ?x ?z) (alive ?x) (not(isclosed ?y))(not(isclosed ?z)))
:effect (and (not (at ?y ?x)) (at ?z ?x) (isclosed ?y) (not (busy ?y)))
)


(:action give
  :parameters (?giver ?taker - person ?t - thing ?p - place)
  :precondition (and (likes ?giver ?taker) (has ?giver ?t) (alive ?giver) (alive ?taker)  (at ?p ?taker) (at ?p ?giver) (not (= ?taker ?giver)))
  :effect (and (has ?taker ?t) (not (has ?giver ?t)))
)

(:action follow
  :parameters (?fw ?fo - person ?p - place)
  :precondition (and (at ?p ?fw) (at ?p ?fo) (not (= ?fo ?fw)))
  :effect (and (isFollowing ?fw ?fo))
)

(:action hear
  :parameters (?fw ?fo - person ?p - securePlace ?w - secretWords)
  :precondition (and (isFollowing ?fw ?fo) (isOpening ?fo ?p ?w))
  :effect (and (knows ?fw ?w))
)

(:action greet
  :parameters (?chara ?charb - character ?p - place)
  :precondition (and (sociable ?chara) (alive ?chara) (alive ?charb) (at ?p ?chara) (at ?p ?charb) (not (= ?chara ?charb)) )
  :effect (and (knows ?chara ?charb) (knows ?charb ?chara) )
)

(:action open
  :parameters (?x - character ?p - securePlace ?w - secretWords)
  :precondition (and (isclosed ?p) (knows ?x ?w) )
  :effect (and (not (isclosed ?p)) (isOpening ?x ?p ?w) )
)

(:action close
  :parameters (?x - character ?p - securePlace)
  :precondition (and (not (isclosed ?p)) (at ?p ?x))
  :effect (and (isclosed ?p) )
)

(:action put
  :parameters (?putter - person ?t - locatable ?p - securePlace)
  :precondition (and (has ?putter ?t) (alive ?putter) (at ?p ?putter) (knows ?putter ?p))
  :effect (and (has ?p ?t) (not (has ?putter ?t)))
)

(:action get
  :parameters (?getter - person ?t - locatable ?p - securePlace)
  :precondition (and (has ?p ?t) (alive ?getter) (at ?p ?getter) (knows ?getter ?p) )
  :effect (and (has ?getter ?t) (not (has ?p ?t)))
)

(:action cut-wood
 :parameters (?c - character ?p - place)
 :precondition (and (works-as ?c woodcutter) (has ?p wood))
 :effect (and (has ?c wood))
)

)
