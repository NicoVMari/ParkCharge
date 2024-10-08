import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class EDF {
    /**
     * @param userRequesting id user requesting to charge
     * @param timeToCharge time in minutes of the requesting charge
     * @param prenotazioni list of prenotazioni
     * @param ricariche list of charges already accepted
     * @return true if charge is acceptable(completable before the end of the prenotazione), false otherwise
     */
    public static boolean isAcceptable(String userRequesting, int timeToCharge, ArrayList<Prenotazioni> prenotazioni, ArrayList<Ricariche> ricariche){
        return isAcceptable(userRequesting, timeToCharge, prenotazioni, ricariche, LocalDateTime.now(), false);
    }

    //ultimoo parametro per testing
    public static boolean isAcceptable(String userRequesting, int timeToCharge, ArrayList<Prenotazioni> prenotazioni, ArrayList<Ricariche> ricariche, LocalDateTime startTime, boolean test){
        Prenotazioni prenotazioneUtente = prenotazioni.stream()
                .filter(p -> p.getUtente().equals(userRequesting))
                .findFirst()
                .orElse(null);

        prenotazioni.sort(Comparator.comparing(Prenotazioni::getTempo_uscita));
        LocalDateTime t = startTime;

        for(Prenotazioni p: prenotazioni){
            //ignoro prenotazioni concluse, col sistema al lavoro non dovrebbe mai accadere, ma nel testing è utile.
            //EDF per ricariche di prenotazioni concluse nel passato (nel database per testing, ma che nella realtà dovrebbero essere nello storico)
            //non funziona, nel caso sia un test non serve
            if(!test && p.getTempo_uscita().isBefore(LocalDateTime.now())) continue;

            if(ricariche.stream().anyMatch(r -> r.getPrenotazione() == p.getId())){

                //ottengo la ricarica in corso associata a una prenotazione
                Ricariche ricaricaPrenotazione = ricariche.stream()
                        .filter(r -> r.getPrenotazione() == p.getId())
                        .filter(r -> r.getPercentuale_erogata() != r.getPercentuale_richiesta())
                        .findFirst()
                        .orElse(null);

                if(ricaricaPrenotazione != null){
                    if(t.plusMinutes(ricaricaPrenotazione.getDurata_ricarica() - ricaricaPrenotazione.getPercentuale_erogata()).isAfter(p.getTempo_uscita())){
                        return false;
                    }
                    t = t.plusMinutes(ricaricaPrenotazione.getDurata_ricarica() - ricaricaPrenotazione.getPercentuale_erogata());
                }

            }

            if(p.equals(prenotazioneUtente)){
                if(t.plusMinutes(timeToCharge).isAfter(p.getTempo_uscita())){
                    return false;
                }
                t = t.plusMinutes(timeToCharge);
            }
        }

        return true;
    }

    /**
     * @param prenotazioni list of prenotazioni
     * @param ricariche list of ricariche
     * @return place number for the MWBot to charge, -1 if MWBot can go idle (nothing to charge)
     */
    public static Prenotazioni getJobPosto(ArrayList<Prenotazioni> prenotazioni, ArrayList<Ricariche> ricariche, boolean isTest){
        //rimuovo prenotazioni conclose (per testing)
        if(!isTest) prenotazioni = (ArrayList<Prenotazioni>) prenotazioni.stream().filter(p -> p.getTempo_uscita().isAfter(LocalDateTime.now())).collect(Collectors.toList());

        //ordino per tempo di uscita
        prenotazioni.sort(Comparator.comparing(Prenotazioni::getTempo_uscita));

        for(Prenotazioni p: prenotazioni){
            if(ricariche.stream().anyMatch(r -> r.getPrenotazione() == p.getId() && r.getPercentuale_richiesta() != r.getPercentuale_erogata())){
                return p;
            }
        }
        return null;
    }
}
