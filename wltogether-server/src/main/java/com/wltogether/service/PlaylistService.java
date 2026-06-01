package com.wltogether.service;

import com.wltogether.model.dto.*;
import com.wltogether.model.entity.Playlist;
import com.wltogether.model.entity.PlaylistTrack;
import com.wltogether.model.entity.User;
import com.wltogether.repository.PlaylistRepository;
import com.wltogether.repository.PlaylistTrackRepository;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final UserRepository userRepository;

    // ===================== Playlist CRUD =====================

    public List<PlaylistResponse> listPlaylists(Long groupId) {
        return playlistRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlaylistResponse getPlaylist(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
    }

    @Transactional
    public PlaylistResponse createPlaylist(Long groupId, Long creatorId, CreatePlaylistRequest request) {
        Playlist playlist = Playlist.builder()
                .groupId(groupId)
                .name(request.getName())
                .creatorId(creatorId)
                .build();
        playlist = playlistRepository.save(playlist);
        return toResponse(playlist);
    }

    @Transactional
    public PlaylistResponse updatePlaylist(Long playlistId, Long userId, String newName) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
        if (!playlist.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("仅歌单创建者可修改");
        }
        playlist.setName(newName);
        playlist = playlistRepository.save(playlist);
        return toResponse(playlist);
    }

    @Transactional
    public void deletePlaylist(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
        if (!playlist.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("仅歌单创建者可删除");
        }
        playlistTrackRepository.deleteByPlaylistId(playlistId);
        playlistRepository.delete(playlist);
    }

    // ===================== Tracks =====================

    public List<TrackResponse> listTracks(Long playlistId) {
        return playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId).stream()
                .map(this::toTrackResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TrackResponse addTrack(Long playlistId, Long userId, AddTrackRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
        if (!playlist.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("仅歌单创建者可添加曲目");
        }

        // Get max sort order
        List<PlaylistTrack> existing = playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId);
        int maxOrder = existing.isEmpty() ? -1 : existing.get(existing.size() - 1).getSortOrder();

        PlaylistTrack track = PlaylistTrack.builder()
                .playlistId(playlistId)
                .fileHash(request.getFileHash())
                .fileName(request.getFileName())
                .durationSeconds(request.getDurationSeconds())
                .artist(request.getArtist())
                .album(request.getAlbum())
                .sortOrder(maxOrder + 1)
                .build();
        track = playlistTrackRepository.save(track);
        return toTrackResponse(track);
    }

    @Transactional
    public void removeTrack(Long trackId, Long userId) {
        PlaylistTrack track = playlistTrackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("曲目不存在"));
        Playlist playlist = playlistRepository.findById(track.getPlaylistId())
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
        if (!playlist.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("仅歌单创建者可删除曲目");
        }
        playlistTrackRepository.delete(track);
    }

    @Transactional
    public List<TrackResponse> reorderTracks(Long playlistId, Long userId, List<Long> trackIds) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("歌单不存在"));
        if (!playlist.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("仅歌单创建者可排序");
        }

        for (int i = 0; i < trackIds.size(); i++) {
            PlaylistTrack track = playlistTrackRepository.findById(trackIds.get(i)).orElse(null);
            if (track != null && track.getPlaylistId().equals(playlistId)) {
                track.setSortOrder(i);
                playlistTrackRepository.save(track);
            }
        }

        return listTracks(playlistId);
    }

    // ===================== Helpers =====================

    private PlaylistResponse toResponse(Playlist playlist) {
        List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(playlist.getId());
        User creator = userRepository.findById(playlist.getCreatorId()).orElse(null);
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .groupId(playlist.getGroupId())
                .name(playlist.getName())
                .creatorId(playlist.getCreatorId())
                .creatorNickname(creator != null ? creator.getNickname() : null)
                .trackCount(tracks.size())
                .tracks(tracks.stream().map(this::toTrackResponse).collect(Collectors.toList()))
                .createdAt(playlist.getCreatedAt())
                .build();
    }

    private TrackResponse toTrackResponse(PlaylistTrack track) {
        return TrackResponse.builder()
                .id(track.getId())
                .playlistId(track.getPlaylistId())
                .fileHash(track.getFileHash())
                .fileName(track.getFileName())
                .durationSeconds(track.getDurationSeconds())
                .artist(track.getArtist())
                .album(track.getAlbum())
                .sortOrder(track.getSortOrder())
                .build();
    }
}
