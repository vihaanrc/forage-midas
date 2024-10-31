package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.entity.UserRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;


    public TransactionListener(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas")
    public void listen(Transaction transaction) {

        if (isValid(transaction)) {
            UserRecord sender;
            UserRecord recipient;
            sender = userRepository.findById(transaction.getSenderId());
            recipient = userRepository.findById(transaction.getRecipientId());
            processTransaction(sender, recipient, transaction.getAmount());
        }
    }

    private boolean isValid(Transaction transaction) {
        // senderID, recipientID are valid, valid balance w.r.t transaction amount.
        UserRecord sender;
        UserRecord recipient;
        float transactionAmount = transaction.getAmount();

        sender = userRepository.findById(transaction.getSenderId());
        recipient = userRepository.findById(transaction.getRecipientId());


        return recipient != null && sender != null && sender.getBalance() >= transactionAmount;
    }

    private void processTransaction(UserRecord sender, UserRecord recipient, float amount) {
        //process transaction and send to UserRepository
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord transactionEntry = new TransactionRecord(sender, recipient, amount);
        transactionRepository.save(transactionEntry);

    }
}
