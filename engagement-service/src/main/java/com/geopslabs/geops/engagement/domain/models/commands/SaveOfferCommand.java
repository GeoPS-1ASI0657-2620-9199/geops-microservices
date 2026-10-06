package com.geopslabs.geops.engagement.domain.models.commands;

/**
 * SaveOfferCommand
 *
 * Command record that encapsulates all the necessary data to create a new saved offer entry.
 * This command validates input data and ensures that required fields are properly provided
 * for saved offer creation
 *
 * @summary Command to create a new saved offer entry
 * @param userId The unique identifier of the user creating the saved offer
 * @param offerId The unique identifier of the offer being saved offer
 *
 * @since 1.0
 * @author GeOps Labs
 */

public record SaveOfferCommand(
        Long userId,
        Long offerId
) {
    /**
     * Compact constructor that validates the command parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public SaveOfferCommand {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null or empty");
        }
    }
}
