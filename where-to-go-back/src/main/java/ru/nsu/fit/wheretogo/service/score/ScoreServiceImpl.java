package ru.nsu.fit.wheretogo.service.score;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.fit.wheretogo.dto.place.PlaceDescriptionDTO;
import ru.nsu.fit.wheretogo.dto.user.ScoreDTO;
import ru.nsu.fit.wheretogo.dto.user.UserDTO;
import ru.nsu.fit.wheretogo.entity.place.Category;
import ru.nsu.fit.wheretogo.entity.place.Place;
import ru.nsu.fit.wheretogo.entity.score.Score;
import ru.nsu.fit.wheretogo.entity.score.ScoreId;
import ru.nsu.fit.wheretogo.entity.user.User;
import ru.nsu.fit.wheretogo.entity.user.coefficient.UserCoefficient;
import ru.nsu.fit.wheretogo.entity.user.coefficient.UserCoefficientId;
import ru.nsu.fit.wheretogo.repository.place.PlaceRepository;
import ru.nsu.fit.wheretogo.repository.score.ScoreRepository;
import ru.nsu.fit.wheretogo.repository.user.coefficient.UserCoefficientMainRepository;

import java.util.List;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final UserCoefficientMainRepository userCoefficientMainRepository;
    private final PlaceRepository placeRepository;

    public ScoreServiceImpl(
            ScoreRepository scoreRepository,
            UserCoefficientMainRepository userCoefficientMainRepository,
            PlaceRepository placeRepository) {
        this.scoreRepository = scoreRepository;
        this.userCoefficientMainRepository = userCoefficientMainRepository;
        this.placeRepository = placeRepository;
    }

    @Override
    @Transactional
    public ScoreDTO createScore(Long userId, Long placeId, Integer value) {
        var oldValue = 0;
        if (scoreRepository.existsByUserIdAndPlaceId(userId, placeId)) {
            var currScore = scoreRepository.findByUserIdAndPlaceId(userId, placeId);
            oldValue = currScore.getScoreValue();
        }

        Score score = scoreRepository.save(new Score()
                .setId(new ScoreId().setUser(userId).setPlace(placeId))
                .setUser(new User().setId(userId))
                .setPlace(new Place().setId(placeId))
                .setScoreValue(value));

        List<Category> placeCategories = placeRepository.findById(placeId).orElseThrow().getCategories();

        for (Category category : placeCategories) {
            if (userCoefficientMainRepository.existsByCategory(category)) {
                if (oldValue != 0) {
                    double newValue = (value - oldValue);
                    userCoefficientMainRepository.updateCoefficientWithoutIncrement(newValue, category.getId(), userId);
                } else {
                    userCoefficientMainRepository.updateCoefficientWithIncrement(
                            value.doubleValue(), category.getId(), userId);
                }

            } else {
                userCoefficientMainRepository.save(new UserCoefficient()
                        .setCoefficientId(new UserCoefficientId().setUser(userId).setCategory(category.getId()))
                        .setUser(new User().setId(userId))
                        .setCategory(category)
                        .setPlacesCount(1L)
                        .setCoefficient(value.doubleValue()));
            }
        }
        return ScoreDTO.getFromEntity(score);
    }

    @Override
    @Transactional
    public void deleteScore(ScoreDTO scoreDTO) {
        scoreRepository.delete(Score.getFromDto(scoreDTO));
    }

    @Override
    @Transactional
    public ScoreDTO getByPlaceScoreFromUser(Long userId, Long placeId) {
        if (userId == null || placeId == null) {
            return null;
        }
        return ScoreDTO.getFromEntity(scoreRepository.findByUserIdAndPlaceId(userId, placeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreDTO> getByUser(UserDTO userDto) {
        List<Score> scores = scoreRepository.findByUserId(User.getFromDTO(userDto).getId());

        return scores
                .stream()
                .map(ScoreDTO::getFromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreDTO> getByPlace(PlaceDescriptionDTO placeDto) {
        List<Score> scores = scoreRepository.findByPlaceId(Place.getFromDTO(placeDto).getId());

        return scores
                .stream()
                .map(ScoreDTO::getFromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ifUserHasScores(Long userId) {
        return scoreRepository.existsByUserId(userId);
    }
}