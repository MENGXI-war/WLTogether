package com.wltogether.controller;

import com.wltogether.model.dto.*;
import com.wltogether.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    // ===================== Playlist CRUD =====================

    @PostMapping("/groups/{groupId}/playlists")
    public ResponseEntity<PlaylistResponse> create(Authentication auth,
                                                    @PathVariable Long groupId,
                                                    @Valid @RequestBody CreatePlaylistRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.createPlaylist(groupId, userId, request));
    }

    @GetMapping("/groups/{groupId}/playlists")
    public ResponseEntity<List<PlaylistResponse>> list(@PathVariable Long groupId) {
        return ResponseEntity.ok(playlistService.listPlaylists(groupId));
    }

    @GetMapping("/playlists/{id}")
    public ResponseEntity<PlaylistResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylist(id));
    }

    @PutMapping("/playlists/{id}")
    public ResponseEntity<PlaylistResponse> update(Authentication auth,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody CreatePlaylistRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(playlistService.updatePlaylist(id, userId, request.getName()));
    }

    @DeleteMapping("/playlists/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        playlistService.deletePlaylist(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("歌单已删除"));
    }

    // ===================== Tracks =====================

    @GetMapping("/playlists/{id}/tracks")
    public ResponseEntity<List<TrackResponse>> listTracks(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.listTracks(id));
    }

    @PostMapping("/playlists/{id}/tracks")
    public ResponseEntity<TrackResponse> addTrack(Authentication auth,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AddTrackRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.addTrack(id, userId, request));
    }

    @DeleteMapping("/playlists/{id}/tracks/{trackId}")
    public ResponseEntity<ApiResponse<Void>> removeTrack(Authentication auth,
                                                          @PathVariable Long id,
                                                          @PathVariable Long trackId) {
        Long userId = (Long) auth.getPrincipal();
        playlistService.removeTrack(trackId, userId);
        return ResponseEntity.ok(ApiResponse.ok("曲目已删除"));
    }

    @PutMapping("/playlists/{id}/tracks/reorder")
    public ResponseEntity<List<TrackResponse>> reorderTracks(Authentication auth,
                                                              @PathVariable Long id,
                                                              @RequestBody List<Long> trackIds) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(playlistService.reorderTracks(id, userId, trackIds));
    }
}
