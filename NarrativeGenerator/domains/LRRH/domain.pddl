(define (domain LRRH)
(:requirements :strips :typing )
(:types place locatable thing level period - talkable
		weapon hood - thing
		beauty - thing
        character - locatable
        animal person supernatural - character )
            
(:predicates 
			 (alive ?x - character)
			 (canwalk ?x - character)
			 (cantalk ?x - character)
			 (canhear ?x - character)
			 (snoring ?x - character)
			 (eaten ?x - character)
             (hungry ?x - character)
             (knows ?x - character ?y - talkable)
             (has ?x - character ?y - thing)
			 (inBelly ?x - character ?y - character)
			 (extroverted ?x - character)
			 (at ?loc - place ?char - character)
			 (connected ?loca ?locb - place)
			 (escaped ?p - person)
			 (armed ?p - person)
			 (mean ?c - character)
			 (good ?c - character)
			 (fond-of ?c1 - character ?c2 - talkable)
			 (isopen ?o - talkable)
			 
			 (hunger ?x - character ?l - level)
			 (fear ?x - character ?y - character ?l - level)
			 
			 (time ?t - timeofday)

			 (masked ?x - character ?y - character)
			 )

(:action walk-to
:parameters (?x - character ?y - place ?z - place)
:precondition (and (at ?y ?x) (canwalk ?x) (alive ?x) (connected ?y ?z) (knows ?x ?z) )
:effect (and (not (at ?y ?x)) (at ?z ?x))
)
			 
(:action tell-about
  :parameters (?speaker ?hearer - character ?topic - talkable ?p - place)
  :precondition (and (knows ?speaker ?hearer) (knows ?speaker ?topic) (not (knows ?hearer ?topic)) (not (= ?hearer ?speaker)) (alive ?speaker) (alive ?hearer) (at ?p ?hearer) (at ?p ?speaker) (cantalk ?speaker) (canhear ?hearer))
  :effect (knows ?hearer ?topic)
)

(:action eat-person
  :parameters (?eater - animal ?eatee - person ?p - place)
  :precondition (and  (alive ?eater) (alive ?eatee) (not (eaten ?eatee)) (at ?p ?eater) (at ?p ?eatee) (alive ?eatee) (not (= ?eater ?eatee))
;( forall (?k - person) (not (and (armed ?k) (at ?p ?k) )))
;	 	(forall (?k - person) 
;	 		(imply (at ?p ?k) (not (armed ?k)) ) )
  )
   :effect (and (eaten ?eatee) (inBelly ?eatee ?eater) (not (hungry ?eater)) (not (cantalk ?eatee)) (not (canwalk ?eatee)) (not (canhear ?eatee)) (not (at ?p ?eatee)))
)

(:action escape-from-belly
:parameters (?escaper - person ?escapee - animal ?p - place)
:precondition (and (inBelly ?escaper ?escapee) (eaten ?escaper) (not (alive ?escapee)) (alive ?escaper) (at ?p ?escapee) )
	:effect (and (not (inBelly ?escaper ?escapee)) (not (eaten ?escaper)) (escaped ?escaper) (cantalk ?escaper) (canwalk ?escaper) (canhear ?escaper) (at ?p ?escaper))
)


(:action kill
  :parameters (?killer ?victim - character ?p - place ?w - weapon)
  :precondition (and (alive ?killer) (alive ?victim) (has ?killer ?w) (at ?p ?killer) (at ?p ?victim) (not (= ?killer ?victim)) (not (fond-of ?killer ?victim)))
  :effect (and (not (alive ?victim)) (not (cantalk ?victim)) (not (canwalk ?victim)) (not (canhear ?victim)) )
)

(:action give
  :parameters (?giver ?taker - person ?t - thing ?p - place)
  :precondition (and (has ?giver ?t) (alive ?giver) (alive ?taker)  (at ?p ?taker) (at ?p ?giver) (not (= ?taker ?giver)) (cantalk ?giver)(canhear ?taker) (fond-of ?giver ?taker))
  :effect (and (has ?taker ?t) (not (has ?giver ?t)) )
)
(:action resurrect
  :parameters (?fairy - supernatural ?body - character)
  :precondition (and (not (alive ?body)) (alive ?fairy) (not (eaten ?fairy)) )
  :effect (and (alive ?body) (cantalk ?body) (canwalk ?body) (canhear ?body) )
 )

(:action greet
  :parameters (?chara ?charb - character ?p - place)
  :precondition (and (extroverted ?chara) (alive ?chara) (alive ?charb) (at ?p ?chara) (at ?p ?charb) (not (= ?chara ?charb)) (cantalk ?chara) (canhear ?charb) (not (eaten ?chara)) (not (eaten ?charb)) )
  :effect (and (knows ?chara ?charb) (knows ?charb ?chara) )
)
(:action close
  :parameters (?x - character ?o - thing)
  :precondition (and (canwalk ?x) (isopen ?o))
  :effect (and (not (isopen ?o)) )
)

(:action open
  :parameters (?x - character ?o - thing)
  :precondition (and (canwalk ?x) (not (isopen ?o)) )
  :effect (and (isopen ?o) )
)

)
